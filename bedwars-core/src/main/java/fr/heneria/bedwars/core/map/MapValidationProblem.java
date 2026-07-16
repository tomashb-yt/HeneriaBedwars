package fr.heneria.bedwars.core.map;

import fr.heneria.bedwars.core.config.ProblemSeverity;

public record MapValidationProblem(ProblemSeverity severity, String code, String action) {}
