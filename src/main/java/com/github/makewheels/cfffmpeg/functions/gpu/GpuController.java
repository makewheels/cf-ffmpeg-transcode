package com.github.makewheels.cfffmpeg.functions.gpu;

import cn.hutool.core.util.IdUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GpuController {
    @PostMapping("hello")
    public String hello() {
        return "this is gpu controller" + IdUtil.simpleUUID();
    }
}
