import org.json.JSONObject;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ShamirSecret {
    static class Point {
        int x;
        BigInteger y;

        Point(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("input.json")));
        JSONObject obj = new JSONObject(content);

        JSONObject keys = obj.getJSONObject("keys");
        int n = keys.getInt("n");
        int k = keys.getInt("k");

        List<Point> points = new ArrayList<>();
        Iterator<String> iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.equals("keys"))
                continue;
            try {
                int x = Integer.parseInt(key);
                JSONObject rootObj = obj.getJSONObject(key);
                int base = rootObj.getInt("base");
                String valueStr = rootObj.getString("value");
                BigInteger y = new BigInteger(valueStr, base);
                points.add(new Point(x, y));
            } catch (NumberFormatException e) {
                System.err.println("Skipping key: " + key);
            }
        }

        if (points.size() != n) {
            System.err.println("Warning: Expected " + n + " points, found " + points.size());
        }

        Collections.sort(points, (a, b) -> a.x - b.x);
        List<Point> selectedPoints = points.subList(0, k);
        BigInteger c = interpolate(selectedPoints);
        System.out.println(c);
    }

    private static BigInteger interpolate(List<Point> points) {
        int k = points.size();
        BigInteger totalNum = BigInteger.ZERO;
        BigInteger totalDen = BigInteger.ONE;

        for (int i = 0; i < k; i++) {
            BigInteger xi = BigInteger.valueOf(points.get(i).x);
            BigInteger yi = points.get(i).y;
            BigInteger num_i = yi;
            BigInteger den_i = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (j == i)
                    continue;
                BigInteger xj = BigInteger.valueOf(points.get(j).x);
                num_i = num_i.multiply(xj.negate());
                den_i = den_i.multiply(xi.subtract(xj));
            }

            totalNum = totalNum.multiply(den_i).add(num_i.multiply(totalDen));
            totalDen = totalDen.multiply(den_i);
        }

        return totalNum.divide(totalDen);
    }
}