package com.tonyostudio.fileextensionscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tonyostudio.library.FileExtSearchService;


public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_REQUEST_CODE = 1;

    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new Adapter();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String[] permissions = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissions, STORAGE_REQUEST_CODE);
        } else {
            runFileExtensionSearchService();
            runAppFileScannerService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_REQUEST_CODE) {

            int granted = android.content.pm.PackageManager.PERMISSION_GRANTED;
            if(grantResults[0] == granted) {
                runFileExtensionSearchService();
                runAppFileScannerService();
            }
        }
    }

    private void runFileExtensionSearchService() {

        String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String dirPath1 = Environment.getExternalStorageDirectory().getAbsolutePath();

        String[] fileExts = getResources().getStringArray(R.array.file_exts);
        String[] dirPaths = new String[] {dirPath,dirPath1};

        Intent intent = FileExtSearchService.newIntent(this,fileExts,dirPaths, FileExtSearchService.EXTRA_ADD,true);

        startService(intent);
    }

    private void runAppFileScannerService() {

        String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String dirPath1 = Environment.getExternalStorageDirectory().getAbsolutePath();

        String[] fileExts = getResources().getStringArray(R.array.file_exts);
        String[] dirPaths = new String[] {dirPath,dirPath1};

        Bundle bundle = AppFileScannerService.newBundle(fileExts,dirPaths,FileExtSearchService.EXTRA_ADD,true);
        Intent intent = new Intent(this,AppFileScannerService.class);
        intent.putExtras(bundle);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = FileExtSearchService.newReceiverIntentFilter();
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(FileExtSearchService.ACTION_SEARCH_COMPLETE)) {

                String[] filePaths = intent.getStringArrayExtra(FileExtSearchService.EXTRA_RESULTS);
                adapter.setFilePaths(filePaths);
            }
        }
    };


    public static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private String[] filePaths;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1,parent,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            String path = filePaths[position];
            holder.textView.setText(path);
        }

        @Override
        public int getItemCount() {

            if(filePaths == null) {
                return 0;
            }

            return filePaths.length;
        }

        public void setFilePaths(@Nullable String[] filePaths) {
            this.filePaths = filePaths;
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
