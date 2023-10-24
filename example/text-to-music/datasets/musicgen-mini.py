from starwhale import dataset
from starwhale.utils.debug import init_logger

init_logger(3)

# data from https://ai.honu.io/papers/musicgen/
desc_samples = [
    "Pop dance track with catchy melodies, tropical percussion, and upbeat rhythms, perfect for the beach",
    "A grand orchestral arrangement with thunderous percussion, epic brass fanfares, and soaring strings, creating a cinematic atmosphere fit for a heroic battle.",
    "classic reggae track with an electronic guitar solo",
    "earthy tones, environmentally conscious, ukulele-infused, harmonic, breezy, easygoing, organic instrumentation, gentle grooves",
    "lofi slow bpm electro chill with organic samples",
    "drum and bass beat with intense percussions",
    "A dynamic blend of hip-hop and orchestral elements, with sweeping strings and brass, evoking the vibrant energy of the city.",
    "violins and synths that inspire awe at the finiteness of life and the universe",
    "80s electronic track with melodic synthesizers, catchy beat and groovy bass",
    "reggaeton track, with a booming 808 kick, synth melodies layered with Latin percussion elements, uplifting and energizing",
    "a piano and cello duet playing a sad chambers music",
    "smooth jazz, with a saxophone solo, piano chords, and snare full drums",
    "a light and cheerly EDM track, with syncopated drums, aery pads, and strong emotions",
    "a punchy double-bass and a distorted guitar riff",
    "acoustic folk song to play during roadtrips: guitar flute choirs",
    "rock with saturated guitars, a heavy bass line and crazy drum break and fills.",
    "90s rock song with electric guitar and heavy drums",
    "An 80s driving pop song with heavy drums and synth pads in the background",
    "An energetic hip-hop music piece, with synth sounds and strong bass. There is a rhythmic hi-hat patten in the drums.",
    "90s rock song with electric guitar and heavy drums",
    "An 80s driving pop song with heavy drums and synth pads in the background",
    "An energetic hip-hop music piece, with synth sounds and strong bass. There is a rhythmic hi-hat patten in the drums."
    "90s rock song with electric guitar and heavy drums",
    "An 80s driving pop song with heavy drums and synth pads in the background",
    "An energetic hip-hop music piece, with synth sounds and strong bass. There is a rhythmic hi-hat patten in the drums.",
]


def build_dataset() -> None:
    print("Building musicgen dataset...")
    with dataset("musicgen-mini") as ds:
        for idx, desc in enumerate(desc_samples):
            ds[idx] = {"desc": desc}
        ds.commit()


if __name__ == "__main__":
    build_dataset()
