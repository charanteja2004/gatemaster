package com.example.gatemaster;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.gatemaster.models.Topic;
import com.google.android.material.appbar.MaterialToolbar;

import androidx.pdf.viewer.fragment.PdfViewerFragment;

public class ContentViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC = "extra_topic";
    public static final String EXTRA_COURSE_TITLE = "extra_course_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_viewer);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Topic topic = (Topic) getIntent().getSerializableExtra(EXTRA_TOPIC);
        String courseTitle = getIntent().getStringExtra(EXTRA_COURSE_TITLE);

        if (topic != null) {
            getSupportActionBar().setTitle(topic.getTitle());
            if (courseTitle != null) {
                getSupportActionBar().setSubtitle(courseTitle);
            }

            WebView webView = findViewById(R.id.webView);
            FrameLayout pdfContainer = findViewById(R.id.pdf_fragment_container);
            TextView emptyPdfText = findViewById(R.id.txt_empty_pdf);

            if (topic.getPdfPath() != null && !topic.getPdfPath().isEmpty()) {
                // Load PDF Viewer Fragment
                webView.setVisibility(View.GONE);
                pdfContainer.setVisibility(View.VISIBLE);
                emptyPdfText.setVisibility(View.GONE);

                PdfViewerFragment pdfViewerFragment = new PdfViewerFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.pdf_fragment_container, pdfViewerFragment);
                transaction.commit();

                // Set Document UI. If it's from assets or remote, we need to pass a uri.
                // Depending on the structure, we pass the correct Uri.
                Uri pdfUri = Uri.parse(topic.getPdfPath());
                pdfViewerFragment.setDocumentUri(pdfUri);

            } else if (topic.getContentPath() != null && !topic.getContentPath().isEmpty()) {
                // Load WebView
                webView.setVisibility(View.VISIBLE);
                pdfContainer.setVisibility(View.GONE);
                emptyPdfText.setVisibility(View.GONE);

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowFileAccess(false);
                webSettings.setAllowFileAccessFromFileURLs(false);
                webSettings.setAllowUniversalAccessFromFileURLs(false);
                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setDisplayZoomControls(false);

                webView.loadUrl("file:///android_asset/" + topic.getContentPath());
            } else {
                // Empty PDF Slot
                webView.setVisibility(View.GONE);
                pdfContainer.setVisibility(View.GONE);
                emptyPdfText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
