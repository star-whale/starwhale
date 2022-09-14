import subprocess
from typing import List, Tuple


def invoke(args: List[str]) -> Tuple[str, str]:
    process = subprocess.run(args,
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE,
                             universal_newlines=True)
    print(f"out:{process.stdout}, error:{process.stderr}")
    return process.stdout, process.stderr


def invoke_with_react(args: List[str], input_content: str = "yes") -> Tuple[str, str]:
    p = subprocess.Popen(args,
                         stdout=subprocess.PIPE, stdin=subprocess.PIPE,
                         stderr=subprocess.PIPE, universal_newlines=True)
    _stdout, _err = p.communicate(input=input_content)
    print(f"out:{_stdout}, error:{_err}")
    return _stdout, _err
