#! /bin/bash

global_index=0
label_index=0

read_dir(){
    for file in `ls $1`
    do
        if [ -d $1"/"$file ]
        then
            read_dir $1"/"$file $label_index
            let label_index++
        else
            echo $global_index $2 ${1:13}"/"$file >> "data/"all_list.txt
            let global_index++
        fi
    done
}

read_dir data/UCF-101
