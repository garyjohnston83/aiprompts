package com.gjjfintech.aiprompts.service;

public record ImageInput(String url) {
    public static ImageInput fromUrl(String url) { return new ImageInput(url); }
}