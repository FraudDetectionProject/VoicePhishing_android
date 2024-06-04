package com.example.voicephising;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    final int TRANSMIT_MODE = 0;
    TextView file_name;
    MediaPlayer mPlayer = new MediaPlayer();
    Uri audioUri;

    // 타임아웃 시간을 설정하기 위한 클라이언트 객체
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3,TimeUnit.MINUTES)
            .writeTimeout(3,TimeUnit.MINUTES)
            .build();
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://nodeiot.duckdns.org:55555")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    api service = retrofit.create(api.class);

    public interface api {

        // 파일을 직접 전송하고 결과를 받아오는 방식
        @Multipart
        @POST("/yh/fraud/transcript")
        Call<Result> uploadFile(@Part MultipartBody.Part file);

        // 파일을 base64로 인코딩해서 보내고 결과를 받아오는 방식
        @POST("/yh/fraud/transcriptjson")
        Call<Result> uploadJson(@Body encodedFile file);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("보이스피싱 판별");

        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},MODE_PRIVATE);

        file_name = findViewById(R.id.file_name); // 오디오 파일의 이름을 표시할 TextView 변수에 참조
        Button bring_audio = findViewById(R.id.bring_file); // 오디오 파일을 탐색하기 위해 클릭할 Button 변수에 참조
        Button play_audio = findViewById(R.id.play_file); // 오디오 파일을 재생하기 위해 클릭할 Button 변수에 참조
        Button transmit_audio = findViewById(R.id.transmit_file); // 오디오 파일을 API에 전송하기 위해 클릭할 Button 변수에 참조
        Button stopplay_audio = findViewById(R.id.stopplay_file);

        LottieAnimationView loading = findViewById(R.id.loading);

        Intent ResultIntend = new Intent(MainActivity.this, ResultActivity.class);

        // 애플리케이션에 파일을 탐색하기 위해 사용할 버튼의 Listener 객체 설정
        bring_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ACTION_GET_CONTENT - 문서나 사진 등의 파일을 선택하고 앱에 그 참조를 반환하기 위해 요청하는 액션
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*"); // 탐색할 파일 MIME 타입 설정

                launcher_audio.launch(intent); // 파일탐색 액션을 가진 인텐트 실행
            }
        });

        // 오디오 파일 재생을 위한 버튼의 Listener 객체 설정
        play_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if (mPlayer.isPlaying()) {
                        mPlayer.stop();
                        mPlayer.reset();
                    }

                    // 파일 첨부 여부 확인
                    if(audioUri != null){
                        // 재생 중단을 위한 버튼 생성
                        stopplay_audio.setVisibility(View.VISIBLE);

                        // 파일 재생 중임을 알리는 토스트 생성
                        Toast toast = new Toast(MainActivity.this);
                        toast.setText("파일 재생 중...");
                        toast.show();

                        // mPlayer 객체에 파일 uri 지정 후 재생
                        mPlayer.setDataSource(MainActivity.this, audioUri);
                        mPlayer.prepare();
                        mPlayer.start();
                    }else{
                        Toast.makeText(MainActivity.this, "파일을 선택하십시오.", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e){
                    // 오류 발생 시 로그를 남기고 오류 토스트 생성
                    System.out.println(e);
                    Toast.makeText(MainActivity.this, "오디오 파일 재생 실패", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 오디오 파일 재생을 멈추기 위해 사용할 버튼의 Listener 객체 설정
        stopplay_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.stop();
                mPlayer.reset();
                // 재생 중단을 위한 버튼 제거
                stopplay_audio.setVisibility(View.INVISIBLE);
            }
        });

        // 파일을 서버에 전송하기 위한 버튼에 Listener 객체 지정
        transmit_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioUri != null) {
                    // 파일 전송 버튼 비활성화
                    transmit_audio.setEnabled(false);
                    loading.setVisibility(View.VISIBLE);
                    File file = getFileFromUri(MainActivity.this, audioUri);
                    // 파일을 MultypartBody로 직접 전달할 때
                    if(TRANSMIT_MODE==0) {
                        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(audioUri)), file);
                        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
                        Call<Result> transmit = service.uploadFile(body);

                        Toast.makeText(MainActivity.this, "파일 전송 중...", Toast.LENGTH_SHORT).show();
                        // 서버 요청
                        transmit.enqueue(new Callback<Result>() {
                            @Override
                            public void onResponse(Call<Result> call, Response<Result> response) {
                                // 파일 전송 성공 시 호출
                                if (response.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "파일 전송 완료", Toast.LENGTH_SHORT).show();
                                    Result r = response.body();
//                                    System.out.println(r.getData());
                                    boolean resultcode = r.getResultCode();

                                    if(resultcode==true){
                                        // 인텐드로 결과 전송 후 결과 액티비티 호출
                                        ResultIntend.putExtra("isFraud", r.getFraudCode());
                                        ResultIntend.putExtra("Prob", r.getProbability());
                                        ResultIntend.putExtra("Text", r.getData());
                                        startActivity(ResultIntend);
                                    }else{
                                        AlertDialog.Builder warningDlg = new AlertDialog.Builder(MainActivity.this);
                                        warningDlg.setTitle(getReason(r.getReasoncode()));
                                        warningDlg.setMessage("파일을 다시 선택하십시오.");
                                        warningDlg.setPositiveButton("확인", null);
                                        warningDlg.show();
                                    }
                                } else {    // 파일 전송 실패 시 호출
                                    Toast.makeText(MainActivity.this, "파일 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    System.out.println(response);
                                }

                                // 파일 전송 버튼 활성화
                                transmit_audio.setEnabled(true);
                                loading.setVisibility(View.INVISIBLE);
                            }
                            @Override
                            // 오류 발생 시 호출
                            public void onFailure(Call<Result> call, Throwable t) {
                                Toast.makeText(MainActivity.this, "파일 전송 시도 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                System.out.println(t);
                                // 파일 전송 버튼 활성화
                                transmit_audio.setEnabled(true);
                                loading.setVisibility(View.INVISIBLE);
                            }
                        });
                    }else {     // 파일을 Base64로 인코딩하여 json 형식으로 보낼때
                        try {
                            // byte로 변환된 파일을 Base64로 인코딩
                            String encodedString = Base64.encodeToString(getBytes(file), Base64.DEFAULT);
                            encodedFile body = new encodedFile(true, encodedString);
                            Call<Result> transmit = service.uploadJson(body);

                            Toast.makeText(MainActivity.this, "파일 전송 중...", Toast.LENGTH_SHORT).show();
                            // 서버 요청
                            transmit.enqueue(new Callback<Result>() {
                                @Override
                                public void onResponse(Call<Result> call, Response<Result> response) {
                                    // 파일 전송 성공 시 호출
                                    if (response.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "파일 전송 완료", Toast.LENGTH_SHORT).show();
                                        Result r = response.body();
//                                        System.out.println(r.getData());

                                        boolean resultcode = r.getResultCode();

                                        if(resultcode==true){
                                            // 인텐드로 결과 전송 후 결과 액티비티 호출
                                            ResultIntend.putExtra("isFraud", r.getFraudCode());
                                            ResultIntend.putExtra("Text", r.getData());
                                            startActivity(ResultIntend);
                                        }else{
                                            AlertDialog.Builder warningDlg = new AlertDialog.Builder(MainActivity.this);
                                            warningDlg.setTitle(getReason(r.getReasoncode()));
                                            warningDlg.setTitle("파일을 다시 선택하십시오.");
                                            warningDlg.setPositiveButton("확인", null);
                                            warningDlg.show();
                                        }
                                    } else {    // 파일 전송 실패 시 호출
                                        Toast.makeText(MainActivity.this, "파일 전송에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                        System.out.println(response);
                                    }

                                    // 파일 전송 버튼 활성화
                                    transmit_audio.setEnabled(true);
                                    loading.setVisibility(View.INVISIBLE);
                                }
                                @Override
                                // 오류 발생 시 호출
                                public void onFailure(Call<Result> call, Throwable t) {
                                    Toast.makeText(MainActivity.this, "파일 전송 시도 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                    System.out.println(t);
                                    // 파일 전송 버튼 활성화
                                    transmit_audio.setEnabled(true);
                                    loading.setVisibility(View.INVISIBLE);
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                // 선택된 파일이 없을 때 호출
                }else Toast.makeText(MainActivity.this, "파일을 선택하십시오.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 오디오 파일 탐색 후 선택했을 때 콜백메서드를 설정한 intent launcher
    ActivityResultLauncher<Intent> launcher_audio = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void onActivityResult(ActivityResult result) { // 사용자가 파일 탐색 화면에서 돌아왔을 때 호출되는 메소드
                    if (result.getResultCode() == Activity.RESULT_OK) { // 사용자가 파일 선택을 성공적으로 완료했을 때 내부 코드 실행
                        Log.d("launcher_audio Callback", "audio picking has succeeded"); // 로그 출력

                        Intent data = result.getData(); // 콜백 메서드를 통해 전달 받은 ActivityResult 객체에서 Intent 객체 추출
                        audioUri = data.getData(); // Intent 객체에서 선택한 오디오 파일의 위치를 가리키는 Uri 추출

                        File audioFile = getFileFromUri(MainActivity.this, audioUri); // Uri를 사용해 파일 복사본 생성
                        if(audioFile != null){ // 파일이 정상적으로 생성되었을 때 내부 코드 실행
                            file_name.setText(audioFile.getName()); // 복사본 파일의 이름으로 TextView에 설정
                        }else{ // 파일이 정상적으로 생성되지 않았을 때 내부 코드 실행
                            Toast.makeText(MainActivity.this, "오디오 파일을 가져오는데 문제가 생겼습니다.", Toast.LENGTH_SHORT).show(); // 메시지 출력
                        }

                    }else if(result.getResultCode() == Activity.RESULT_CANCELED){ // 사용자가 파일 탐색 중 선택을 하지 않았을 때 내부 코드 실행
                        Log.d("launcher_audio Callback", "audio picking is canceled"); // 로그 출력
                    }else{ // 그 외의 경우 예외 처리
                        Log.e("launcher_audio Callback", "audio picking has failed"); // 로그 출력
                    }
                }
            });

    // URI를 사용해 파일을 복사하고 복사한 경로를 제공하는 메소드
    public static File getFileFromUri(Context context, Uri uri) {
        // 앱과 안드로이드 시스템 간의 데이터 통신을 하기위해 ContentResolver 객체 생성
        // ContentResolver를 통해 앱은 ContentProvider를 사용해 다른 앱의 데이터에 접근하거나 데이터를 읽거나 쓸 수 있다
        ContentResolver contentResolver = context.getContentResolver();
        // Uri를 사용해 파일 이름 반환
        String fileName = getFileName(contentResolver, uri);

        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        // 같은 이름으로 앱 내부저장소에 파일 생성
        File file = new File(context.getCacheDir(), fileName);

        try {
            inputStream = contentResolver.openInputStream(uri); // uri에 있는 데이터를 읽기 위해 InputStream을 열고 InputStream을 반환한다
            if (inputStream == null) {
                return null; // InputStream을 여는데 실패할 경우 null 반환
            }

            outputStream = new FileOutputStream(file); // 이전에 만든 File 객체에 데이터를 쓰기 위해 OutputStream 생성
            byte[] buffer = new byte[4 * 1024]; // 4 KB buffer 생성
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) { // inputStream을 사용해 데이터를 읽고 읽은 데이터가 있다면 while문 내부 코드 실행
                outputStream.write(buffer, 0, bytesRead); // 읽은 byte 데이터를 사용해 File 객체에 데이터 쓰기
            }

            return file; // 그 후 복사가 완료된 파일 객체 반환
        } catch (IOException e) { // 입출력 문제 생기면 오류 출력
            e.printStackTrace();
            return null;
        } finally {
            // 마지막으로 사용한 inputStream, outputStream 종료
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) { // 입출력 문제 생기면 오류 출력
                e.printStackTrace();
            }
        }
    }

    // uri를 사용해 파일 이름을 반환하는 메서드
    private static String getFileName(ContentResolver contentResolver, Uri uri) {
        // ContentProvider 를 통해 기기 데이터베이스에서 데이터를 조회하기 위해 query() 메서드 사용
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        String displayName = null;
        if (cursor != null && cursor.moveToFirst()) { // 조회된 데이터를 저장한 cursor가 null이 아니고 첫번째 레코드로 이동할 수 있으면(첫번째 레코드가 없다면 false 반환) 내부 코드 실행
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); // 파일이름이 저장된 열의 위치 반환 후 저장
            if (nameIndex != -1) { // 해당 열이 존재할 경우 내부 코드 실행
                displayName = cursor.getString(nameIndex); // 열에 있는 데이터 반환
            }
            cursor.close(); // 커서 종료
        }
        return displayName; // 받은 파일이름 반환
    }

    // 파일을 byte 형식으로 읽기 위한 함수
    public static byte[] getBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file); // 파일 입력 스트림
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(); // byte 쓰기 스트림
        int bufferSize = 2048; // 버퍼 사이즈
        byte[] buffer = new byte[bufferSize]; // byte로 변환한 파일을 담기 위한 버퍼 정의

        int len;
        // 파일을 읽어들여 byte 형태로 변환
        while ((len = fis.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        fis.close();
        byteBuffer.close();
        return byteBuffer.toByteArray();
    }

    public static String getReason(int reasoncode){
        switch (reasoncode){
            case 1:
                return "파일 길이가 너무 짧습니다.";
            case 2:
                return "잘못된 파일 형식입니다.";
            default:
                return "";
        }
    }
}