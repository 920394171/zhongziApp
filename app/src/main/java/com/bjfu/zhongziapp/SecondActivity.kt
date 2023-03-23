package com.bjfu.zhongziapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.FloatBuffer
import java.util.*

class SecondActivity : AppCompatActivity(), Runnable {
    private val segmentationInstance = SeedSegmentation()
    private val mImageIndex = 0
    private val mTestImages = arrayOf("test1.png", "test2.jpg", "test3.png")
    private var mImageView: ImageView? = null
    private var mButtonDetect: Button? = null
    private var mButtonReturn: Button? = null
    private var mProgressBar: ProgressBar? = null
    private var mTextView: TextView? = null
    private var mBitmap: Bitmap? = null
    private var mModule: Module? = null
    private var mInputTensorBuffer: FloatBuffer? = null
    private var mInputTensor: Tensor? = null
    private val INPUT_TENSOR_WIDTH = 224
    private val INPUT_TENSOR_HEIGHT = 224
    private val TOP_K = 2
    private val mImgScaleX = 0f
    private val mImgScaleY = 0f
    private val mIvScaleX = 0f
    private val mIvScaleY = 0f
    private val mStartX = 0f
    private val mStartY = 0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //这个是获取布局文件的，这里是你下一个页面的布局文件//注意这个是跳转界面的不能设置错，应该是第一个

        // 权限获取
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
        setContentView(R.layout.second)
        try {
            mBitmap = BitmapFactory.decodeStream(assets.open(mTestImages[mImageIndex]))
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }
        mImageView = findViewById(R.id.imageView)
        //        mImageView.setImageBitmap(mBitmap);
        val buttonSelect = findViewById<Button>(R.id.selectButton)
        buttonSelect.setOnClickListener { v: View? ->
            val options = arrayOf<CharSequence>("选择相册", "拍摄图片", "取消")
            val builder = AlertDialog.Builder(this@SecondActivity)
            builder.setTitle("选择新图片")
            builder.setItems(options) { dialog: DialogInterface, item: Int ->
                if (options[item] == "拍摄图片") {
                    val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePicture, 0)
                } else if (options[item] == "选择相册") {
                    val pickPhoto =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhoto, 1)
                } else if (options[item] == "取消") {
                    dialog.dismiss()
                }
            }
            builder.show()
        }
        mButtonDetect = findViewById(R.id.detectButton)
        mProgressBar = findViewById(R.id.progressBar)
        mButtonDetect!!.setOnClickListener(View.OnClickListener { v: View? ->
            mButtonDetect!!.isEnabled = false
            mProgressBar!!.visibility = ProgressBar.VISIBLE
            mButtonDetect!!.text = getString(R.string.run_model)

//            mImgScaleX = (float)mBitmap.getWidth() / PrePostProcessor.mInputWidth;
//            mImgScaleY = (float)mBitmap.getHeight() / PrePostProcessor.mInputHeight;
//
//            mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageView.getWidth() / mBitmap.getWidth() : (float)mImageView.getHeight() / mBitmap.getHeight());
//            mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageView.getHeight() / mBitmap.getHeight() : (float)mImageView.getWidth() / mBitmap.getWidth());
//
//            mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
//            mStartY = (mImageView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;
            val thread = Thread(this@SecondActivity)
            thread.start()
        })
        mTextView = findViewById(R.id.textView)
        mButtonReturn = findViewById(R.id.returnButton)
        mButtonReturn!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
        try {
            val moduleName = "shufflenet.pt"
            //            String moduleName = "model0.pt";
            mModule = LiteModuleLoader.load(assetFilePath(applicationContext, moduleName))
        } catch (e: IOException) {
            Log.e(TAG, "无法加载模型", e)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            when (requestCode) {
                0 -> if (resultCode == RESULT_OK && data != null) {
                    mBitmap = data.extras!!["data"] as Bitmap?
                    val matrix = Matrix()
                    //                        matrix.postRotate(-90.0f);
                    mBitmap = Bitmap.createBitmap(mBitmap!!, 0, 0, mBitmap!!.width, mBitmap!!.height, matrix, true)
                    mImageView!!.setImageBitmap(mBitmap)
                }
                1 -> if (resultCode == RESULT_OK && data != null) {
                    val selectedImage = data.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    if (selectedImage != null) {
                        val cursor = contentResolver.query(
                            selectedImage,
                            filePathColumn, null, null, null
                        )
                        if (cursor != null) {
                            cursor.moveToFirst()
                            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                            val picturePath = cursor.getString(columnIndex)
                            mBitmap = BitmapFactory.decodeFile(picturePath)
                            val matrix = Matrix()
                            //                                matrix.postRotate(90.0f);
                            mBitmap = Bitmap.createBitmap(mBitmap!!, 0, 0, mBitmap!!.getWidth(), mBitmap!!.getHeight(), matrix, true)
                            mImageView!!.setImageBitmap(mBitmap)
                            cursor.close()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        runOnUiThread {
            var seedNum = 0
            var allImgNum = 1
            if (mModule != null) {

//                mImageView?.setImageBitmap(mBitmap)
                val imgArray = segmentationInstance.seg(mBitmap, this.filesDir)
                val pairs = loadModuleToGetJingkeRate(imgArray)
                var seedInfos: String = ""
                allImgNum = pairs.values.sum()
                for (pair in pairs) {
                    seedNum = pair.value
                    seedInfos += "${pair.key}含量：${100.0 * seedNum / allImgNum}%(${seedNum}/${allImgNum})\n"
                }
                mTextView!!.text = seedInfos

                mButtonDetect!!.isEnabled = true
                mProgressBar!!.visibility = ProgressBar.INVISIBLE
                mButtonDetect!!.setText(R.string.detect)
            }
        }
    }

    private fun loadModuleToGetJingkeRate(imgStrings: ArrayList<String>): MutableMap<String, Int> {
        val seeds: MutableMap<String, Int> = mutableMapOf()
        for (imgPath in imgStrings) {
            mInputTensorBuffer = Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT)
            mInputTensor = Tensor.fromBlob(
                mInputTensorBuffer, longArrayOf(1, 3, INPUT_TENSOR_HEIGHT.toLong(), INPUT_TENSOR_WIDTH.toLong())
            )

            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            var bitmap = BitmapFactory.decodeFile(imgPath, options)
            if (imgPath.contains("output.jpg")) {
                mImageView?.setImageBitmap(bitmap)
            }

//            var bitmap = pair.second
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT, false)
            TensorImageUtils.bitmapToFloatBuffer(
                bitmap,
                0, 0,
                bitmap.width,
                bitmap.height,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                mInputTensorBuffer,
                0
            )
            val outputTensor = mModule!!.forward(IValue.from(mInputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray
            val ixs = topK(scores, TOP_K)
            val topKClassNames = arrayOfNulls<String>(TOP_K)
            val topKScores = FloatArray(TOP_K)
            for (i in 0 until TOP_K) {
                val ix = ixs[i]
                topKClassNames[i] = Constants.IMAGENET_CLASSES[ix]
                topKScores[i] = scores[ix]
                Log.e(
                    TAG,
                    "${imgPath}: ix:" + ix + "--className:" + topKClassNames[i] + "--score:" + topKScores[i]
                )
                if (topKScores[i] > 0 && !imgPath.contains("output.jpg")) {
                    val value = seeds[Constants.IMAGENET_CLASSES[ix]] ?: 0
                    seeds[Constants.IMAGENET_CLASSES[ix]] = value + 1
                }
            }
        }
        for (img in imgStrings) {
            val file = File(img)
            file.delete()
        }

        for(i in 0 until TOP_K){
            val value = seeds[Constants.IMAGENET_CLASSES[i]] ?: 0
            seeds[Constants.IMAGENET_CLASSES[i]] = value
        }

        return seeds
    }

    private fun topK(a: FloatArray, topk: Int): IntArray {
        val values = FloatArray(topk)
        Arrays.fill(values, -Float.MAX_VALUE)
        val ixs = IntArray(topk)
        Arrays.fill(ixs, -1)
        for (i in a.indices) {
            for (j in 0 until topk) {
                if (a[i] > values[j]) {
                    for (k in topk - 1 downTo j + 1) {
                        values[k] = values[k - 1]
                        ixs[k] = ixs[k - 1]
                    }
                    values[j] = a[i]
                    ixs[j] = i
                    break
                }
            }
        }
        return ixs
    }

    companion object {
        private const val TAG = "zhongzi"

        // for yolov5 model, no need to apply MEAN and STD
        var NO_MEAN_RGB = floatArrayOf(0.0f, 0.0f, 0.0f)
        var NO_STD_RGB = floatArrayOf(1.0f, 1.0f, 1.0f)

        /**
         * 访问文件方法
         *
         * @param context
         * @param assetName
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        fun assetFilePath(context: Context, assetName: String): String {
            val file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
            context.assets.open(assetName).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (`is`.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        }
    }
}
