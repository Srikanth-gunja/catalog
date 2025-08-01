
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.*;

public class SecretSharing {

    static class Point {
        BigInteger x;
        BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        BigInteger getX() {
            return x;
        }

        BigInteger getY() {
            return y;
        }
    }

    static class Fraction {
        BigInteger num;
        BigInteger den;

        Fraction(BigInteger num, BigInteger den) {
            this.num = num;
            this.den = den;
        }

        Fraction add(Fraction other) {
            BigInteger newNum = this.num.multiply(other.den).add(other.num.multiply(this.den));
            BigInteger newDen = this.den.multiply(other.den);
            return new Fraction(newNum, newDen);
        }
    }

    public static void main(String[] args) {
        try {
            // Read JSON from file
            String jsonString = new Scanner(new FileReader("input.json")).useDelimiter("\\Z").next();

            // Parse JSON
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(jsonString, type);

            // Extract n and k
            Map<String, Object> keys = (Map<String, Object>) data.get("keys");
            int n = ((Double) keys.get("n")).intValue();
            int k = ((Double) keys.get("k")).intValue();

            // Collect and decode points
            List<Point> points = new ArrayList<>();
            for (String key : data.keySet()) {
                if (!key.equals("keys")) {
                    BigInteger x = new BigInteger(key);
                    Map<String, String> pointData = (Map<String, String>) data.get(key);
                    int base = Integer.parseInt(pointData.get("base"));
                    String valueStr = pointData.get("value");
                    BigInteger y = new BigInteger(valueStr, base);
                    points.add(new Point(x, y));
                }
            }

            // Sort points by x for consistency
            points.sort(Comparator.comparing(Point::getX));

            // Print decoded y-values
            System.out.println("Decoded y-values:");
            for (Point p : points) {
                System.out.println("x = " + p.x + ", y = " + p.y);
            }

            // Select first k points
            List<Point> selectedPoints = points.subList(0, k);

            // Compute p(0) using Lagrange interpolation
            Fraction sum = new Fraction(BigInteger.ZERO, BigInteger.ONE);
            for (int i = 0; i < k; i++) {
                BigInteger xi = selectedPoints.get(i).x;
                BigInteger yi = selectedPoints.get(i).y;
                BigInteger prodNum = BigInteger.ONE;
                BigInteger prodDen = BigInteger.ONE;
                for (int j = 0; j < k; j++) {
                    if (j != i) {
                        BigInteger xj = selectedPoints.get(j).x;
                        prodNum = prodNum.multiply(xj.negate());
                        prodDen = prodDen.multiply(xi.subtract(xj));
                    }
                }
                BigInteger num_i = yi.multiply(prodNum);
                BigInteger den_i = prodDen;
                Fraction t_i = new Fraction(num_i, den_i);
                sum = sum.add(t_i);
            }

            // Verify sum is an integer and compute c
            if (!sum.num.mod(sum.den).equals(BigInteger.ZERO)) {
                throw new RuntimeException("Result is not an integer");
            }
            BigInteger c = sum.num.divide(sum.den);

            // Print the constant term
            System.out.println("Constant term c: " + c);

        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
        }
    }
}
