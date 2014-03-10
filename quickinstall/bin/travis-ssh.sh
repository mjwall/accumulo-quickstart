#!/bin/bash

sudo start ssh
ssh-keygen -t rsa -f ~/.ssh/id_rsa -N "" -q
cat ~/.ssh/id_rsa.pub >>~/.ssh/authorized_keys
ssh-keyscan -t rsa localhost >>~/.ssh/known_hosts
