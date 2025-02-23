package com.dpeng.gateway.common.enums;

import lombok.Getter;

@Getter
public enum ConfigCenterEnum {

    NACOS("nacos"),
    ZOOKEEPER("zookeeper");

    private final String des;

    ConfigCenterEnum(String des) {
        this.des = des;
    }

}
