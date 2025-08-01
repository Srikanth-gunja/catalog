import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class ShamirSecretSharing {
    
    public static void main(String[] args) {
        try {
            // Test both cases
            System.out.println("=== Test Case 1 ===");
            String secret1 = solveSecretSharing("testcase1.json");
            System.out.println("Secret: " + secret1);
            
            System.out.println("\n=== Test Case 2 ===");
            String secret2 = solveSecretSharing("testcase2.json");
            System.out.println("Secret: " + secret2);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String solveSecretSharing(String filename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File(filename));
        
        // Parse keys
        JsonNode keysNode = rootNode.get("keys");
        int n = keysNode.get("n").asInt();
        int k = keysNode.get("k").asInt();
        
        System.out.println("n = " + n + ", k = " + k);
        
        // Parse and decode points
        List<Point> points = new ArrayList<>();
        
        for (int i = 1; i <= n; i++) {
            JsonNode pointNode = rootNode.get(String.valueOf(i));
            if (pointNode != null) {
                int base = pointNode.get("base").asInt();
                String value = pointNode.get("value").asText();
                
                BigInteger x = BigInteger.valueOf(i);
                BigInteger y = decodeValue(value, base);
                
                points.add(new Point(x, y));
                System.out.println("Point " + i + ": x=" + x + ", y=" + y + 
                                 " (decoded from base " + base + ")");
            }
        }
        
        // Use first k points for interpolation
        List<Point> selectedPoints = points.subList(0, k);
        
        // Calculate secret using Lagrange interpolation
        BigInteger secret = lagrangeInterpolation(selectedPoints);
        
        return secret.toString();
    }
    
    public static BigInteger decodeValue(String value, int base) {
        return new BigInteger(value, base);
    }
    
    public static BigInteger lagrangeInterpolation(List<Point> points) {
        BigInteger result = BigInteger.ZERO;
        int n = points.size();
        
        for (int i = 0; i < n; i++) {
            BigInteger xi = points.get(i).x;
            BigInteger yi = points.get(i).y;
            
            // Calculate Lagrange basis polynomial Li(0)
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    BigInteger xj = points.get(j).x;
                    numerator = numerator.multiply(BigInteger.ZERO.subtract(xj)); // 0 - xj
                    denominator = denominator.multiply(xi.subtract(xj)); // xi - xj
                }
            }
            
            // Calculate yi * Li(0)
            BigInteger term = yi.multiply(numerator).divide(denominator);
            result = result.add(term);
        }
        
        return result;
    }
    
    static class Point {
        BigInteger x, y;
        
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
}