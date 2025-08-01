
// File: SecretReconstructor.java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SecretReconstructor {

    public static void main(String[] args) {
        String filePath = "testcase1.json"; // Default to testcase1.json
        if (args.length >= 1) {
            filePath = args[0];
        }

        try {
            // Step 1: Parse the JSON file and decode the points.
            InputData input = parseJsonFile(filePath);
            List<Point> allPoints = decodePoints(input.points);
            int k = input.keys.k;

            // Ensure we have enough points to meet the threshold k.
            if (allPoints.size() < k) {
                throw new IllegalArgumentException(
                        "Not enough points to reconstruct secret. Have " + allPoints.size() + ", need " + k);
            }

            // Select the first k points for reconstruction.
            List<Point> sharesForReconstruction = allPoints.subList(0, k);
            System.out.println("Using the following " + k + " shares for reconstruction:");
            sharesForReconstruction.forEach(System.out::println);

            // Step 2: Reconstruct the secret using Lagrange Interpolation.
            BigInteger secret = reconstructSecret(sharesForReconstruction);

            // Step 3: Print the final results.
            System.out.println("\n--- Reconstruction Complete ---");
            System.out.println("The reconstructed secret y = f(0) is: " + secret);
            System.out.println("The constant c is: " + secret);

        } catch (IOException e) {
            System.err.println("Error reading or parsing JSON file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error decoding number values: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON file into our POJO data model using Jackson.
     * 
     * @param filePath The path to the JSON file.
     * @return An InputData object representing the parsed JSON.
     * @throws IOException If there is an error reading the file.
     */
    private static InputData parseJsonFile(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = mapper.readValue(new File(filePath), Map.class);

        InputData input = new InputData();

        // Parse keys
        Map<String, Object> keysMap = (Map<String, Object>) jsonMap.get("keys");
        input.keys = new Keys();
        input.keys.n = ((Number) keysMap.get("n")).intValue();
        input.keys.k = ((Number) keysMap.get("k")).intValue();

        // Parse points
        input.points = new HashMap<>();
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("keys")) {
                Map<String, Object> pointMap = (Map<String, Object>) entry.getValue();
                PointValue pv = new PointValue();
                pv.base = pointMap.get("base").toString();
                pv.value = pointMap.get("value").toString();
                input.points.put(key, pv);
            }
        }

        return input;
    }

    /**
     * Decodes the raw point data from the JSON into a list of Point objects.
     * Each point's x and y values are converted to BigInteger.
     * The y-value is decoded from its specified base.
     * 
     * @param pointsMap The map of string keys to PointValue objects from the JSON.
     * @return A sorted list of decoded Point objects.
     */
    private static List<Point> decodePoints(Map<String, PointValue> pointsMap) {
        return pointsMap.entrySet().stream()
                .map(entry -> {
                    // The x-coordinate is the key of the JSON object.
                    BigInteger x = new BigInteger(entry.getKey());
                    PointValue pv = entry.getValue();
                    // The y-coordinate is the value, decoded from the specified base.
                    // The BigInteger constructor handles various bases, including hex (16). [14]
                    BigInteger y = new BigInteger(pv.value, Integer.parseInt(pv.base));
                    return new Point(x, y);
                })
                // Sort points by x-coordinate for consistent processing.
                .sorted((p1, p2) -> p1.x.compareTo(p2.x))
                .collect(Collectors.toList());
    }

    /**
     * Reconstructs the secret S = f(0) using Lagrange Interpolation.
     * This method implements the optimized formula for f(0).
     * 
     * @param shares A list of k points (shares) to use for reconstruction.
     * @return The reconstructed secret as a BigInteger.
     */
    public static BigInteger reconstructSecret(List<Point> shares) {
        BigInteger secret = BigInteger.ZERO;
        int k = shares.size();

        // This implements the formula: S = sum_{j=0}^{k-1} y_j * L_j(0)
        for (int j = 0; j < k; j++) {
            Point currentPoint = shares.get(j);
            BigInteger y_j = currentPoint.y;

            // Calculate the Lagrange basis polynomial L_j(0)
            // L_j(0) = product_{i=0, i!=j}^{k-1} (-x_i) / (x_j - x_i)
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int i = 0; i < k; i++) {
                if (i == j) {
                    continue; // Skip the case where i = j
                }
                Point otherPoint = shares.get(i);
                BigInteger x_i = otherPoint.x;
                BigInteger x_j = currentPoint.x;

                // Numerator part: multiply by -x_i
                numerator = numerator.multiply(x_i.negate());

                // Denominator part: multiply by (x_j - x_i)
                denominator = denominator.multiply(x_j.subtract(x_i));
            }

            // Calculate the full term for this share: y_j * (numerator / denominator)
            // NOTE: For the simplified problem, standard BigInteger division is used.
            // For a secure SSS implementation, this would involve modular inverse.
            // The secure equivalent would be:
            // BigInteger term =
            // y_j.multiply(numerator).multiply(denominator.modInverse(primeModulus));
            // All calculations would be done modulo primeModulus.
            BigInteger lagrangeTerm = y_j.multiply(numerator).divide(denominator);

            // Add this term to the total secret.
            secret = secret.add(lagrangeTerm);
        }

        return secret;
    }

    // Data classes for JSON parsing
    public static class InputData {
        public Keys keys;
        public Map<String, PointValue> points;
    }

    public static class Keys {
        public int n;
        public int k;
    }

    public static class PointValue {
        public String base;
        public String value;
    }

    public static class Point {
        public BigInteger x;
        public BigInteger y;

        public Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point(x=" + x + ", y=" + y + ")";
        }
    }
}