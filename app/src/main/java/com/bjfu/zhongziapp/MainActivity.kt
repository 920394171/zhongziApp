package com.bjfu.zhongziapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {
    private var mAnimationView: LottieAnimationView? = null
    private var button: Button? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAnimationView = findViewById<View>(R.id.animation_view) as LottieAnimationView

        //获取按钮
        button = findViewById<View>(R.id.button_in) as Button


        //按钮进行监听
        button!!.setOnClickListener { //监听按钮，如果点击，就跳转
            val intent = Intent()
            //前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
            intent.setClass(this@MainActivity, SecondActivity::class.java)
            startActivity(intent)
        }
    }

    public override fun onStart() {
        super.onStart()
        mAnimationView?.progress = 0f
        mAnimationView?.playAnimation()
    }

    public override fun onStop() {
        super.onStop()
        mAnimationView?.cancelAnimation()
    }
}