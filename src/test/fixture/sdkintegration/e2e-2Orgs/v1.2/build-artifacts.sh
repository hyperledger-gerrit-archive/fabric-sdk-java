#!/bin/bash

configtxgen -outputBlock orderer.block -profile TwoOrgsOrdererGenesis_v12
configtxgen -outputCreateChannelTx foo.tx -profile TwoOrgsChannel_v12 -channelID foo
configtxgen -outputCreateChannelTx bar.tx -profile TwoOrgsChannel_v12 -channelID bar
