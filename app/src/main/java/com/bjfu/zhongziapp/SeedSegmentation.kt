package com.bjfu.zhongziapp

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

class SeedSegmentation {
    private val TAG: String = "zhongzi"

    fun seg(mBitmap: Bitmap?, externalDirPath: File): ArrayList<String> {
        // Load OpenCV library
        System.loadLibrary("opencv_java4")
        val inputImage = Mat()
        Utils.bitmapToMat(mBitmap, inputImage)
        val grayImage = Mat()
        Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY)
        val binaryImage = Mat()
        Imgproc.threshold(grayImage, binaryImage, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)


        /*Core.bitwise_not(binaryImage, binaryImage);
        Imgcodecs.imwrite("xxx.jpg", binaryImage);
*/
        val morphImage = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(binaryImage, morphImage, Imgproc.MORPH_OPEN, kernel)
        Imgproc.morphologyEx(morphImage, morphImage, Imgproc.MORPH_CLOSE, kernel)
        val invertedImage = Mat()
        Core.bitwise_not(morphImage, invertedImage)
        val contourImage = Mat()
        invertedImage.copyTo(contourImage)
        val contours: List<MatOfPoint> = ArrayList()
        Imgproc.findContours(contourImage, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val outputDir: File = File(externalDirPath, "seeds")
        if (!outputDir.exists()) {
            /*目录不存在 则创建*/
            outputDir.mkdirs()
        }
        Log.e(TAG, "seg: ${outputDir.absolutePath}")


//        val bitmaps = ArrayList<Pair<Int, Bitmap>>()
        val imgs = ArrayList<String>()
        for (i in contours.indices) {
            // Create a bounding box around the seed
            val bbox = Imgproc.boundingRect(contours[i])
            val seedImage = inputImage.submat(bbox)
            val gray = Mat()
            Imgproc.cvtColor(seedImage, gray, Imgproc.COLOR_BGR2GRAY)
            val binary = Mat()
            Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
            val contours1: List<MatOfPoint> = ArrayList()
            Imgproc.findContours(binary, contours1, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            val mask = Mat.zeros(seedImage.size(), seedImage.type())
            Imgproc.drawContours(mask, contours1, -1, Scalar(255.0, 255.0, 255.0), -1)
            val result = Mat()
            Core.bitwise_and(seedImage, mask, result)
            val bitmapResult = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(result, bitmapResult)
//            bitmaps.add(Pair(i + 1, bitmapResult))

            val filename: String = "seed_" + (i + 1) + ".jpg"
            if (i != contours.size - 1)
                Imgcodecs.imwrite(outputDir.absolutePath + "/" + filename, result)

            Imgproc.rectangle(inputImage, bbox.tl(), bbox.br(), Scalar(0.0, 255.0, 0.0), 2)
            if (i != contours.size - 1)
                imgs.add(outputDir.absolutePath + "/" + filename)
        }

        // Save
        Imgcodecs.imwrite(outputDir.absolutePath + "/output.jpg", inputImage)
        imgs.add(outputDir.absolutePath + "/output.jpg")
        return imgs;
    }
}