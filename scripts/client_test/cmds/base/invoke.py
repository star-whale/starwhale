import subprocess
from typing import List, Tuple


def invoke(args: List[str]) -> Tuple[str, str]:
    print(args)
    process = subprocess.run(
        args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    )
    if process.stderr:
        print(f"args:{args}, error is:{process.stderr}")
    return process.stdout, process.stderr


def invoke_with_react(args: List[str], input_content: str = "yes") -> Tuple[str, str]:
    p = subprocess.Popen(
        args,
        stdout=subprocess.PIPE,
        stdin=subprocess.PIPE,
        stderr=subprocess.PIPE,
        universal_newlines=True,
    )
    _stdout, _err = p.communicate(input=input_content)
    if _err:
        print(f"args:{args}, error is:{_err}")
    return _stdout, _err
