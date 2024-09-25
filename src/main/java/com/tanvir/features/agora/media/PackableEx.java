package com.tanvir.features.agora.media;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
