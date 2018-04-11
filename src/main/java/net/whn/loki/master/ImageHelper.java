package net.whn.loki.master;

import com.icafe4j.image.reader.TGAReader;
import com.sun.media.jai.codec.*;
import net.whn.loki.common.Task;
import net.whn.loki.helpers.TGAWriter;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ImageHelper {

    private static final Logger log = Logger.getLogger(ImageHelper.class.toString());

    public static void compositeTiles(File tileFolder, Task task) {

        List<File> inputFiles = getImagesIfAllExists(tileFolder, task);

        if (inputFiles.isEmpty()) {
            return;
        }

        try {
            processTiledFiles(task, inputFiles);
            cleanup(tileFolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<File> getImagesIfAllExists(File tileFolder, Task task) {

        int tilesPerFrame = task.getTilesPerFrame();
        List<File> inputFiles = new ArrayList<>(tilesPerFrame);

        for (int i = 0; i < tilesPerFrame; i++) {
            File file = new File(tileFolder, i + task.getGeneratedOutputFileExtension());
            inputFiles.add(file);
            if (!file.isFile()) {
                log.warning("expected tile file doesn't exist: " + file.getAbsolutePath());
                inputFiles.clear();
                return inputFiles;
            }
        }
        return inputFiles;
    }

    private static File getOutputFile(Task task) {
        String renderedFileName = task.getOutputFilePrefix() + task.getRenderedFileAttributes().get(0).getFile().getName();
        return new File(new File(task.getOutputFolderName()), renderedFileName);
    }


    private static void processTiledFiles(Task task, List<File> images) throws Exception {
        List<BufferedImage> bufferedImages;
        BufferedImage finalImg;
        switch (task.getImageFormatPython()) {
            case "TIFF":
                bufferedImages = convertTiffToBufferedImages(images);
                finalImg = composeTiledImages(task.getTileMultiplier(), bufferedImages);
                saveBufferedImageToTiff(finalImg, getOutputFile(task));
                break;
            case "PNG":
                bufferedImages = convertToBufferedImages(images);
                finalImg = composeTiledImages(task.getTileMultiplier(), bufferedImages);
                saveBufferedImageTo(finalImg, getOutputFile(task), "png");
                break;
            case "TARGA":
            case "TARGA_RAW":
                bufferedImages = convertTgaToBufferedImages(images);
                finalImg = composeTiledImages(task.getTileMultiplier(), bufferedImages);
                saveBufferedImageToTag(finalImg, getOutputFile(task));
                break;
            case "JPEG":
                bufferedImages = convertToBufferedImages(images);
                finalImg = composeTiledImages(task.getTileMultiplier(), bufferedImages);
                saveBufferedImageTo(finalImg, getOutputFile(task), "jpg");
                break;
            case "OPEN_EXR":
                throw new Exception("OPEN_EXR is not supported yet!");
//                System.out.println("OPEN_EXR");
//                bufferedImages = new ArrayList<>(images.size());
//                System.out.println(bufferedImages.size());
        }
    }

    private static void saveBufferedImageToTag(BufferedImage finalImg, File outputFile) throws IOException {
        int width = finalImg.getWidth();
        int height = finalImg.getHeight();
        int [] pixels = finalImg.getRGB(0, 0, width, height, null, 0, width);

        byte [] buffer = TGAWriter.write(pixels, width, height, net.whn.loki.helpers.TGAReader.ARGB);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(buffer);
        }
    }

    /**
     * Because in the application, tileMultiplier has always the same value
     * Because using of tiling with tileMultiplier, will produce a quantity of tiled files equals to (tileMultiplier *2)
     * , then there is no need to initialize local variables rows:int and columns:int, because it's evident that both will be equals to tileMultiplier
     * @param tileMultiplier
     * @param bufferedImages
     * @return
     */
    private static BufferedImage composeTiledImages(int tileMultiplier, List<BufferedImage> bufferedImages) {
        BufferedImage bufferedImage = bufferedImages.get(0);
        int chunkWidth = bufferedImage.getWidth();
        int chunkHeight = bufferedImage.getHeight();

        BufferedImage finalImage = new BufferedImage(chunkWidth * tileMultiplier, chunkHeight * tileMultiplier, BufferedImage.TYPE_INT_ARGB);

        // (it's cropped from bottom to top but it's composited from top to bottom)
        for (int row = tileMultiplier - 1, column = 0; row >= 0; row--) {
            for (int order = 0; order < tileMultiplier; order++) {
                int index = row * tileMultiplier + order;
                int x = chunkWidth * order;
                int y = chunkHeight * column;
                finalImage.createGraphics().drawImage(bufferedImages.get(index), x, y, null);
            }
            column++;
        }
        return finalImage;
    }

    /**
     *
     * @param finalImg
     * @param outputFile
     * @param formatName - could be "png" or "jpg"
     * @throws IOException
     */
    private static void saveBufferedImageTo(BufferedImage finalImg, File outputFile, String formatName) throws IOException {
        ImageIO.write(finalImg, "png", outputFile);
    }

    private static void saveBufferedImageToTiff(BufferedImage finalImg, File outputFile) throws IOException {
        TIFFEncodeParam params = new TIFFEncodeParam();
        OutputStream outputStream = new FileOutputStream(outputFile); // task.getGeneratedOutputFileExtensionWithoutDot()
        ImageEncoder encoder = ImageCodec.createImageEncoder("tiff", outputStream, params);

        params.setExtraImages(Arrays.asList(finalImg).iterator());
        encoder.encode(finalImg);
        outputStream.close();
    }

    /**
     * https://stackoverflow.com/questions/1514035/java-tga-loader
     * @param images
     * @return
     * @throws IOException
     */
    private static List<BufferedImage> convertTgaToBufferedImages(List<File> images) throws Exception {
        List<BufferedImage> bufferedImages = new ArrayList<>(images.size());

        TGAReader tgaReader = new TGAReader();
        for (File image : images) {
            BufferedImage bufferedImage = tgaReader.read(new FileInputStream(image));
            bufferedImages.add(bufferedImage);
        }
        return bufferedImages;
    }

    /**
     * Used for PNG or JPEG
     * @param images
     * @return
     * @throws IOException
     */
    private static List<BufferedImage> convertToBufferedImages(List<File> images) throws IOException {
        List<BufferedImage> bufferedImages = new ArrayList<>(images.size());
        for (File image : images) {
            bufferedImages.add(ImageIO.read(image));
        }
        return bufferedImages;
    }

    private static List<BufferedImage> convertTiffToBufferedImages(List<File> images) throws IOException {
        List<BufferedImage> bufferedImages = new ArrayList<>(images.size());
        for (File image : images) {
            SeekableStream seekableStream = new FileSeekableStream(image);
            ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", seekableStream, null);
            PlanarImage planarImage = new NullOpImage(decoder.decodeAsRenderedImage(0), null, null, OpImage.OP_IO_BOUND);
            BufferedImage bufferedImage = planarImage.getAsBufferedImage();
            bufferedImages.add(bufferedImage);
            seekableStream.close();
        }
        return bufferedImages;
    }

    private static void cleanup(File tileFolder) throws IOException {
        FileUtils.deleteDirectory(tileFolder);
    }

    private static BufferedImage combineImages(BufferedImage bufferedImages[]) {
        int divisions = (int)Math.sqrt((double)bufferedImages.length);
        int actualImage = 0;
        // first we stablish the width and height of the final image
        int finalWidth = 0;
        int finalHeight = 0;
        for (int i = 0; i < divisions; i++){
            finalWidth += bufferedImages[i].getWidth();
            finalHeight += bufferedImages[i*divisions].getHeight();
        }
//        BufferedImage finalImg = new BufferedImage(finalWidth, finalHeight, bufferedImages[0].getType());
        BufferedImage finalImg = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);

        int rowWidth = 0;
        int rowHeight = 0;
        for (int heightImage = 0; heightImage < divisions; heightImage++) {
            for (int widthImage = 0; widthImage < divisions; widthImage++) {
                // check every image
                if (bufferedImages[actualImage] == null) {
                    log.warning("bufferedImages element has null parameter");
                    return null;
                }
                // adding to the final image
                finalImg.createGraphics().drawImage(bufferedImages[actualImage], rowWidth, rowHeight, null);  
                rowWidth += bufferedImages[actualImage].getWidth();
                actualImage++;  
            }  
            // after processing the row we get the height of the last processed image 
            // (it's the same for all in the row) and locate at the begining of the row
            rowHeight += bufferedImages[actualImage - 1].getHeight();
            rowWidth = 0;
        }  
        
        return finalImg;
    }
}