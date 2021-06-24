package com.example.beacontest

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.*


class MainActivity : AppCompatActivity(), BeaconConsumer {
    private var beaconManager: BeaconManager? = null

    // 감지된 비콘들을 임시로 담을 리스트
    private val beaconList: MutableList<Beacon> = ArrayList()

    lateinit var handler : Handler
    var permission_state = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //1. Permission(권한)을 먼저 체크 - 음성기능권한(RECORD_AUDIO), 위치기능권한(ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION)
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
        ) {
            //2. 권한이 없는 경우 권한을 설정하는 메시지를 띄운다.
            permission_state = false
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO),
                    1000)
        } else {
            permission_state = true
        }


        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this)
        var textView = findViewById<TextView>(R.id.textView)

        // 여기가 중요한데, 기기에 따라서 setBeaconLayout 안의 내용을 바꿔줘야 하는듯 싶다.
        // 필자의 경우에는 아래처럼 하니 잘 동작했음.
        beaconManager!!.beaconParsers
            .add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"))

        // 비콘 탐지를 시작한다. 실제로는 서비스를 시작하는것.
        beaconManager!!.bind(this)

        handler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                Log.d("mylog", "핸들러 진입 / beaconList.size: ${beaconList.size}msg:${msg}")

                textView.text = ""

                // 비콘의 아이디와 거리를 측정하여 textView에 넣는다.
                for (beacon in beaconList) {
                    Log.d("mylog", "포문 진입")
                    textView.append(
                        "ID : " + beacon.id2 + " / " + "Distance : " + String.format(
                            "%.3f",
                            beacon.distance
                        ).toDouble() + "m\n"
                    )
                }
                // 자기 자신을 1초마다 호출
                this.sendEmptyMessageDelayed(0, 1000)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.size > 0) { //권한 처리 결과를 확인하고 요청한 요청 코드가 맞으면
            var check_result = true
            permission_state = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result != true) {
                Toast.makeText(this, "권한 설정이 거부되었습니다.\n설정에서 권한을 허용해야 합니다..", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "권한 설정이 되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager!!.unbind(this)
        Log.d("mylog", "APP 종료")
    }

    override fun onBeaconServiceConnect() {
        beaconManager!!.removeAllRangeNotifiers()
        beaconManager!!.addRangeNotifier(object : RangeNotifier {
            override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
                // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
                // region에는 비콘들에 대응하는 Region 객체가 들어온다.
                Log.d("mylog", "addRangeNotifier 진입, 사이즈: ${beacons!!.size}")
                if (beacons!!.size > 0) {
                    Log.d("mylog", "비콘 감지 OK, size:${beacons!!.size}")
                }
                if (beacons.size > 0) {
                    beaconList.clear()
                    for (beacon in beacons) {
                        beaconList.add(beacon)
                    }
                }
            }
        })

        try{
            Log.d("mylog", "비콘매니저 monitor실행")
            beaconManager!!.startRangingBeaconsInRegion(
                    Region(
                            "myRangingUniqueId",
                            null,
                            null,
                            null
                    )
            )
        } catch (e: RemoteException) {
        }
    }

    // 버튼이 클릭되면 textView 에 비콘들의 정보를 뿌린다.
    fun OnButtonClicked(view: View?) {
        // 아래에 있는 handleMessage를 부르는 함수. 맨 처음에는 0초간격이지만 한번 호출되고 나면
        // 1초마다 불러온다.
        Log.d("mylog", "버튼 클릭")
        handler.sendEmptyMessage(0)
    }
}


