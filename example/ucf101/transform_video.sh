#! /bin/bash

read_dir(){
    for file in `ls $1`
    do
        if [ -d $1"/"$file ]
        then
            mkdir -p data/UCF-101-WEBM/${1:13}"/"$file
            read_dir $1"/"$file
        else
            ffmpeg -i $1"/"$file -y data/UCF-101-WEBM/${1:13}"/"${file%.*}".webm"
        fi
    done
}

read_dir data/UCF-101
