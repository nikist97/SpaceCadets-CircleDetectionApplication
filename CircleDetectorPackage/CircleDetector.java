package CircleDetectorPackage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.sql.Time;

public class CircleDetector extends JFrame {

    private int maxX;
    private int maxY;
    private int maxR;
    private int maxOccurrencesForR;
    private int maxSearchR;
    private int minSearchR;

    private CircleDetector(String path, int minSearchR, int maxSearchR){
        try {
            BufferedImage image = ImageIO.read(new File(path));
            BufferedImage resizedImage = new BufferedImage(250, 250, image.getType());
            Graphics2D graphics = resizedImage.createGraphics();
            graphics.drawImage(image, 0, 0, 250, 250, null);
            graphics.dispose();
            image = resizedImage;

            int width = image.getWidth();
            int height = image.getHeight();
            this.init(width * 2, height + 50);
            this.setSearchField(minSearchR, maxSearchR);

            JLabel labelImage = this.createLabelImage(image);

            JLabel grayLabelImage = this.createGrayLabelImage(image);

            BufferedImage originalImage = image;
            image = this.blurImage(image);

            BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            BufferedImage copyImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

            ColorConvertOp colorConvertOp = new ColorConvertOp(image.getColorModel().getColorSpace(), grayImage.getColorModel().getColorSpace(),
                    null);
            colorConvertOp.filter(image, grayImage);
            colorConvertOp.filter(image, copyImage);



            int[][] edges = this.sobelOperator(grayImage,copyImage,width,height);
            this.houghTransform(width,height,edges, this.minSearchR, this.maxSearchR);

            Graphics graphicsCircle = originalImage.getGraphics();
            graphicsCircle.setColor(Color.BLUE);
            for(int radius = this.maxR - 2; radius < this.maxR + 3; radius++) {
                graphicsCircle.drawOval(this.maxX - radius, this.maxY - radius, radius * 2, radius * 2);
            }

            originalImage.setRGB(maxX, maxY, 255);

            System.out.println("X radius = " + this.maxX + " Y radius = " + this.maxY + " Radius = " + this.maxR
                    + " Occurrences = " + this.maxOccurrencesForR);

            labelImage.setIcon(new ImageIcon(originalImage));
            this.getContentPane().add(labelImage);
            grayLabelImage.setIcon(new ImageIcon(grayImage));
            this.getContentPane().add(grayLabelImage);
            this.setVisible(true);

        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void init(int width, int height) {
        this.setSize(width, height);
        this.setTitle("Circle Detector");
        this.setLayout(null);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void setSearchField(int minR, int maxR){
        if (minR > 0 && maxR > 0){
            this.minSearchR = minR;
            this.maxSearchR = maxR;
        }
    }

    private JLabel createLabelImage(BufferedImage image){
        JLabel labelImage = new JLabel();
        labelImage.setBounds(0, 0, image.getWidth(), image.getHeight());
        labelImage.setOpaque(true);
        labelImage.setVisible(true);
        return labelImage;
    }

    private JLabel createGrayLabelImage(BufferedImage image){
        JLabel grayLabelImage = new JLabel();
        grayLabelImage.setBounds(image.getWidth(), 0, image.getWidth() * 2, image.getHeight());
        grayLabelImage.setOpaque(true);
        grayLabelImage.setVisible(true);
        return grayLabelImage;
    }

    private int[][] sobelOperator(BufferedImage grayImage, BufferedImage copyImage, int width, int height){
        int[][] edges = new int[width][height];
        int kernel_x[][] = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int kernel_y[][] = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int x = 1; x < width - 2; x++) {
            for (int y = 1; y < height - 2; y++) {
                int pixel_x = (kernel_x[0][0] * (copyImage.getRGB(x + 1, y + 1))) + (kernel_x[0][1] * copyImage.getRGB(x+1, y)) + (kernel_x[0][2] * copyImage.getRGB(x + 1, y - 1)) +
                        (kernel_x[1][0] * copyImage.getRGB(x, y+1)) + (kernel_x[1][1] * copyImage.getRGB(x, y)) + (kernel_x[1][2] * copyImage.getRGB(x, y - 1)) +
                        (kernel_x[2][0] * copyImage.getRGB(x - 1, y + 1)) + (kernel_x[2][1] * copyImage.getRGB(x - 1, y)) + (kernel_x[2][2] * copyImage.getRGB(x-1, y-1));
                int pixel_y = (kernel_y[0][0] * copyImage.getRGB(x + 1, y + 1)) + (kernel_y[0][1] * copyImage.getRGB(x + 1, y)) + (kernel_y[0][2] * copyImage.getRGB(x + 1, y - 1)) +
                        (kernel_y[1][0] * copyImage.getRGB(x, y + 1)) + (kernel_y[1][1] * copyImage.getRGB(x, y)) + (kernel_y[1][2] * copyImage.getRGB(x, y-1)) +
                        (kernel_y[2][0] * copyImage.getRGB(x - 1, y + 1)) + (kernel_y[2][1] * copyImage.getRGB(x - 1, y)) + (kernel_y[2][2] * copyImage.getRGB(x - 1, y - 1));

                int val = pixel_x + pixel_y;
                //int val = (int) Math.sqrt(pixel_x*pixel_x + pixel_y*pixel_y);
                if (val > 2000000) {
                    grayImage.setRGB(x, y, 255);
                    edges[x][y] = 1;
                } else {
                    grayImage.setRGB(x, y, 0);
                }
            }
        }
        return edges;
    }

    private void houghTransform(int width, int height, int[][] edges, int rMin, int rMax){
        int[][] accumulatorSpace;
        int maxOccurrences = 0;
        int oldX = 0;
        int oldY = 0;

        for (int r = rMin; r < rMax; r++) {
            accumulatorSpace = new int[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if(edges[x][y] == 1) {
                        for (int theta = 1; theta < 360; theta++) {
                            double t = (theta * 3.14) / 180;
                            int xRadius = (int) Math.round(x - r * Math.cos(t));
                            int yRadius = (int) Math.round(y - r * Math.sin(t));
                            if (xRadius < width && xRadius > 0 && yRadius < height && yRadius > 0 && !(xRadius == oldX || yRadius == oldY)) {
                                accumulatorSpace[xRadius][yRadius] += 1;
                                if (accumulatorSpace[xRadius][yRadius] > maxOccurrences) {
                                    maxOccurrences = accumulatorSpace[xRadius][yRadius];
                                    this.maxX = xRadius;
                                    this.maxY = yRadius;
                                }
                            }
                            oldX = xRadius;
                            oldY = yRadius;
                        }
                    }
                }
            }
            if(maxOccurrences > this.maxOccurrencesForR){
                this.maxOccurrencesForR = maxOccurrences;
                this.maxR = r;
            }
        }
    }

    private BufferedImage blurImage(BufferedImage image){
        float[] matrix = new float[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = 1.0f / 30.0f;
        }
        BufferedImageOp op = new ConvolveOp( new Kernel(3, 3, matrix));
        return op.filter(image, null);
    }

    public static void main(String[] args){
        long startTime = System.currentTimeMillis();
        CircleDetector circleDetector = new CircleDetector(args[0], 5, 116);
        long endTime   = System.currentTimeMillis();
        System.out.println((endTime - startTime)/1000);
    }

}
