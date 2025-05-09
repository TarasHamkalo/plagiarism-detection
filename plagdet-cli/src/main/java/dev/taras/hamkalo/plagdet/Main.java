package dev.taras.hamkalo.plagdet;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {
    PlagdetCli.run(args);
  }
}