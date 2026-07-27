package com.takehome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.takehome.core.GeometryUtils;
import com.takehome.core.ValidationUtils;
import com.takehome.model.InputData;
import com.takehome.model.PlacementResult;
import com.takehome.placer.GreedyPlacer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Entry point for the rectangle placement algorithm.
 * Usage: java -jar rectangle-placement.jar <input.json>
 */
public class App {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar rectangle-placement.jar <input.json>");
            System.err.println("Example: java -jar rectangle-placement.jar examples/example1.json");
            System.exit(1);
        }

        String inputPath = args[0];

        try {
            // Read input JSON
            Path path = Paths.get(inputPath);
            String jsonContent = Files.readString(path);
            InputData inputData = MAPPER.readValue(jsonContent, InputData.class);

            // Run placement algorithm
            PlacementResult result = GreedyPlacer.place(inputData);

            // Validate result
            if (result.isFeasible()) {
                double[][] boundary = GeometryUtils.toDoubleArray(inputData.getBoundary());
                double doorStartX = inputData.getDoor().get(0)[0];
                double doorStartY = inputData.getDoor().get(0)[1];
                double doorEndX = inputData.getDoor().get(1)[0];
                double doorEndY = inputData.getDoor().get(1)[1];

                List<String> errors = ValidationUtils.validate(
                        boundary, doorStartX, doorStartY, doorEndX, doorEndY,
                        inputData.isOpenInward(), inputData.getAlgoToPlace(), result);

                if (!errors.isEmpty()) {
                    System.err.println("VALIDATION ERRORS:");
                    for (String err : errors) {
                        System.err.println("  - " + err);
                    }
                }
            }

            // Output result
            String outputJson = MAPPER.writeValueAsString(result);
            System.out.println(outputJson);

        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}