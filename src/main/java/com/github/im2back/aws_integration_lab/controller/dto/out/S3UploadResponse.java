package com.github.im2back.aws_integration_lab.controller.dto.out;



public record S3UploadResponse(
        String bucket,
        String message,
        String path,
        String key
) {}