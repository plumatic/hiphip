#!/bin/bash
cd `dirname $0`
for out in float int long
do
	sed "s/double/${out}/g" <"../java/hiphip/double_/JavaBaseline.java" >"../java/hiphip/${out}_/JavaBaseline.java"
done