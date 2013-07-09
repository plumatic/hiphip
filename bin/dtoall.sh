#!/bin/bash
cd `dirname $0`
for out in float int long
do
	for f in Baseline Helpers
	do
		sed "s/double/${out}/g" <"../java/hiphip/double_/${f}.java" >"../java/hiphip/${out}_/${f}.java"
	done
done