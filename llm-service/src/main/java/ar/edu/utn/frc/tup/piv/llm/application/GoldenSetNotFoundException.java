package ar.edu.utn.frc.tup.piv.llm.application;

import java.util.UUID;
public class GoldenSetNotFoundException extends RuntimeException {
  public GoldenSetNotFoundException(UUID id) { super("Golden set no encontrado: " + id); }
}
