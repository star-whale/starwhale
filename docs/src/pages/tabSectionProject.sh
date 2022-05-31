#（输入comand生成dataset示意图）

# MNIST目录下，根据dataset.yaml内容，构建swds
2 # dataset.yaml 内容可以按需修改
3 swcli dataset build<dataset uri> [working dir] .
4 
5 # 查看构建的swds
6 swcli dataset list[project uri]
7 
8 # push swds到controller中
9 swcli dataset push mnist:hbsgeyzxmq4deytfgy3gin3bhbrxo5a