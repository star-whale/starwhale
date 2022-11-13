import os
import logging

import cv2
import numpy as np
import torch.utils.data as data


class Video(object):
    """basic Video class"""

    def __init__(self, vid_path):
        self.cap = None
        self.faulty_frame = None
        self.frame_count = None
        self.vid_path = None
        self.open(vid_path)

    def __del__(self):
        self.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.__del__()

    def reset(self):
        self.close()
        self.vid_path = None
        self.frame_count = -1
        self.faulty_frame = None
        return self

    def open(self, vid_path):
        assert os.path.exists(vid_path), f"VideoIter:: cannot locate: `{vid_path}'"

        # close previous video & reset variables
        self.reset()

        # try to open video
        cap = cv2.VideoCapture(vid_path)
        if cap.isOpened():
            self.cap = cap
            self.vid_path = vid_path
        else:
            raise IOError(f"VideoIter:: failed to open video: `{vid_path}'")

        return self

    def count_frames(self, check_validity=False):
        offset = 0
        if self.vid_path.endswith(".flv"):
            offset = -1
        unverified_frame_count = int(self.cap.get(cv2.CAP_PROP_FRAME_COUNT)) + offset
        if check_validity:
            verified_frame_count = 0
            for i in range(unverified_frame_count):
                self.cap.set(cv2.CAP_PROP_POS_FRAMES, i)
                if not self.cap.grab():
                    logging.warning(
                        f"VideoIter:: >> frame (start from 0) {i} corrupted in {self.vid_path}"
                    )
                    break
                verified_frame_count = i + 1
            self.frame_count = verified_frame_count
        else:
            self.frame_count = unverified_frame_count
        assert (
            self.frame_count > 0
        ), f"VideoIter:: Video: `{self.vid_path}' has no frames"
        return self.frame_count

    def extract_frames(self, ids, force_color=True):
        frames = self.extract_frames_fast(ids, force_color)
        if frames is None:
            # try slow method:
            frames = self.extract_frames_slow(ids, force_color)
        return frames

    def extract_frames_fast(self, ids, force_color=True):
        assert self.cap is not None, "No opened video."
        if len(ids) < 1:
            return []

        frames = []
        pre_idx = max(ids)
        for idx in ids:
            assert (self.frame_count < 0) or (
                idx < self.frame_count
            ), f"ids: {ids} > total valid frames({self.frame_count})"
            if pre_idx != (idx - 1):
                self.cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            res, frame = self.cap.read()  # in BGR/GRAY format
            pre_idx = idx
            if not res:
                self.faulty_frame = idx
                return None
            if len(frame.shape) < 3:
                if force_color:
                    # Convert Gray to RGB
                    frame = cv2.cvtColor(frame, cv2.COLOR_GRAY2RGB)
            else:
                # Convert BGR to RGB
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            frames.append(frame)
        return frames

    def extract_frames_slow(self, ids, force_color=True):
        assert self.cap is not None, "No opened video."
        if len(ids) < 1:
            return []

        frames = [None] * len(ids)
        idx = min(ids)
        self.cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        while idx <= max(ids):
            res, frame = self.cap.read()  # in BGR/GRAY format
            if not res:
                # end of the video
                self.faulty_frame = idx
                return None
            if idx in ids:
                # fond a frame
                if len(frame.shape) < 3:
                    if force_color:
                        # Convert Gray to RGB
                        frame = cv2.cvtColor(frame, cv2.COLOR_GRAY2RGB)
                else:
                    # Convert BGR to RGB
                    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                pos = [k for k, i in enumerate(ids) if i == idx]
                for k in pos:
                    frames[k] = frame
            idx += 1
        return frames

    def close(self):
        if hasattr(self, "cap") and self.cap is not None:
            self.cap.release()
            self.cap = None
        return self


class VideoIter(data.Dataset):
    def __init__(
        self,
        video_prefix,
        txt_list,
        sampler,
        video_transform=None,
        name="<NO_NAME>",
        force_color=True,
        cached_info_path=None,
        return_item_subpath=False,
        shuffle_list_seed=None,
        check_video=False,
        tolerant_corrupted_video=None,
    ):
        super(VideoIter, self).__init__()
        # load params
        self.sampler = sampler
        self.force_color = force_color
        self.video_prefix = video_prefix
        self.video_transform = video_transform
        self.return_item_subpath = return_item_subpath
        self.backup_item = None
        if (not check_video) and (tolerant_corrupted_video is None):
            tolerant_corrupted_video = True
        self.tolerant_corrupted_video = tolerant_corrupted_video
        self.rng = np.random.RandomState(shuffle_list_seed if shuffle_list_seed else 0)
        # load video list
        self.video_list = self._get_video_list(
            video_prefix=video_prefix,
            txt_list=txt_list,
            check_video=check_video,
            cached_info_path=cached_info_path,
        )
        if shuffle_list_seed is not None:
            self.rng.shuffle(self.video_list)
        logging.info(
            f"VideoIter:: iterator initialized (phase: '{name}', num: {len(self.video_list)})"
        )

    def getitem_from_raw_video(self, index):
        # get current video info
        v_id, label, vid_subpath, frame_count = self.video_list[index]
        video_path = os.path.join(self.video_prefix, vid_subpath)

        faulty_frames = []
        successful_trial = False
        try:
            with Video(vid_path=video_path) as video:
                if frame_count < 0:
                    frame_count = video.count_frames(check_validity=False)
                for i_trial in range(20):
                    # dynamic sampling
                    sampled_ids = self.sampler.sampling(
                        range_max=frame_count, v_id=v_id, prev_failed=(i_trial > 0)
                    )
                    if set(list(sampled_ids)).intersection(faulty_frames):
                        continue
                    # extracting frames
                    sampled_frames = video.extract_frames(
                        ids=sampled_ids, force_color=self.force_color
                    )
                    if sampled_frames is None:
                        faulty_frames.append(video.faulty_frame)
                    else:
                        successful_trial = True
                        break
        except IOError as e:
            logging.warning(f">> I/O error({e.errno}): {e.strerror}")

        if not successful_trial:
            assert (
                self.backup_item is not None
            ), f"VideoIter:: >> frame {faulty_frames} is error & backup is inavailable. [{video_path}]'"
            logging.warning(
                f">> frame {faulty_frames} is error, use backup item! [{video_path}]"
            )
            with Video(vid_path=self.backup_item["video_path"]) as video:
                sampled_frames = video.extract_frames(
                    ids=self.backup_item["sampled_ids"], force_color=self.force_color
                )
        elif self.tolerant_corrupted_video:
            # assume the error rate less than 10%
            if (self.backup_item is None) or (self.rng.rand() < 0.1):
                self.backup_item = {
                    "video_path": video_path,
                    "sampled_ids": sampled_ids,
                }

        clip_input = np.concatenate(sampled_frames, axis=2)
        # apply video augmentation
        if self.video_transform is not None:
            clip_input = self.video_transform(clip_input)

        # print(f"clip_input is:{clip_input}")
        return clip_input, label, vid_subpath

    def __getitem__(self, index):
        success = False
        while not success:
            try:
                clip_input, label, vid_subpath = self.getitem_from_raw_video(index)
                success = True
            except Exception as e:
                index = self.rng.choice(range(0, self.__len__()))
                logging.warning(
                    f"VideoIter:: ERROR!! (Force using another index:\n{index})\n{e}"
                )

        if self.return_item_subpath:
            return clip_input, label, vid_subpath
        else:
            return clip_input, label

    def __len__(self):
        return len(self.video_list)

    def _get_video_list(
        self, video_prefix, txt_list, check_video=False, cached_info_path=None
    ):
        # formate:
        # [vid, label, video_subpath, frame_count]
        assert os.path.exists(
            video_prefix
        ), f"VideoIter:: failed to locate: `{video_prefix}'"
        assert os.path.exists(txt_list), f"VideoIter:: failed to locate: `{txt_list}'"

        # try to load cached list
        cached_video_info = {}
        if cached_info_path:
            if os.path.exists(cached_info_path):
                f = open(cached_info_path, "r")
                cached_video_prefix = f.readline().split()[1]
                cached_txt_list = f.readline().split()[1]
                if (cached_video_prefix == video_prefix.replace(" ", "")) and (
                    cached_txt_list == txt_list.replace(" ", "")
                ):
                    logging.info(
                        f"VideoIter:: loading cached video info from: `{cached_info_path}'"
                    )
                    lines = f.readlines()
                    for line in lines:
                        video_subpath, frame_count = line.split()
                        cached_video_info.update({video_subpath: int(frame_count)})
                else:
                    logging.warning(
                        ">> Cached video list mismatched: "
                        f"(prefix:{cached_video_prefix}, list:{cached_txt_list}) != "
                        f"expected (prefix:{video_prefix}, list:{txt_list})"
                    )
                f.close()
            else:
                if not os.path.exists(os.path.dirname(cached_info_path)):
                    os.makedirs(os.path.dirname(cached_info_path))

        # building dataset
        video_list = []
        new_video_info = {}
        logging_interval = 100
        with open(txt_list) as f:
            lines = f.read().splitlines()
            logging.info(f"VideoIter:: found {len(lines)} videos in `{txt_list}'")
            for i, line in enumerate(lines):
                v_id, label, video_subpath = line.split()
                video_path = os.path.join(video_prefix, video_subpath)
                if not os.path.exists(video_path):
                    logging.warning(f"VideoIter:: >> cannot locate `{video_path}'")
                    continue
                if check_video:
                    if video_subpath in cached_video_info:
                        frame_count = cached_video_info[video_subpath]
                    elif video_subpath in new_video_info:
                        frame_count = cached_video_info[video_subpath]
                    else:
                        with Video(vid_path=video_path) as video:
                            frame_count = video.open(video_path).count_frames(
                                check_validity=True
                            )
                        new_video_info.update({video_subpath: frame_count})
                else:
                    frame_count = -1
                # [3417, 91, 'TennisSwing/v_TennisSwing_g02_c05.avi', -1]
                info = [int(v_id), int(label), video_subpath, frame_count]
                video_list.append(info)
                if check_video and (i % logging_interval) == 0:
                    logging.info(
                        f"VideoIter:: - Checking: {i}/{len(lines)}, \tinfo: {info}"
                    )

        # caching video list
        if cached_info_path and len(new_video_info) > 0:
            logging.info(
                f"VideoIter:: adding {len(new_video_info)} lines new video info to: {cached_info_path}"
            )
            cached_video_info.update(new_video_info)
            with open(cached_info_path, "w") as f:
                # head
                f.write(f"video_prefix: {video_prefix.replace(' ', '')}\n")
                f.write(f"txt_list: {txt_list.replace(' ', '')}\n")
                # content
                for i, (video_subpath, frame_count) in enumerate(
                    cached_video_info.items()
                ):
                    if i > 0:
                        f.write("\n")
                    f.write(f"{video_subpath}\t{frame_count}")

        return video_list
