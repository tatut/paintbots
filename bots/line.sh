#!/bin/bash

source _utils.sh

DIR=$1
AMOUNT=$2

for ((i=0;i<$AMOUNT;i++))
do
    move $DIR
    paint
done
