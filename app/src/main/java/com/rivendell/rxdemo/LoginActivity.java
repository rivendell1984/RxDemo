package com.rivendell.rxdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.rivendell.rxdemo.databinding.ActivityLoginBinding;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding mBinding;
    private boolean isLoging = false;//only change value in main thread
    private Disposable mDispose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        mBinding.password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mBinding.signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mBinding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLoging) {
                    dispose();
                    showProgress(false);
                    isLoging = false;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose();
    }

    @Override
    public void onBackPressed() {
        if (isLoging) {
            dispose();
            showProgress(false);
            isLoging = false;
            return;
        }
        super.onBackPressed();
    }


    private void attemptLogin() {

        if (isLoging) {
            return;
        }

        isLoging = true;

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Reset errors.
        mBinding.username.setError(null);
        mBinding.password.setError(null);

        // Store values at the time of the login attempt.
        String username = mBinding.username.getText().toString();
        String password = mBinding.password.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mBinding.password.setError(getString(R.string.error_invalid_password));
            focusView = mBinding.password;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mBinding.username.setError(getString(R.string.error_field_required));
            focusView = mBinding.username;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mBinding.username.setError(getString(R.string.error_invalid_username));
            focusView = mBinding.username;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            isLoging = false;
        } else {
            showProgress(true);

            Observable.create(new ObservableOnSubscribe<Integer>() {

                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                    Timber.i("subscribing");
                    e.onError(new RuntimeException("always fails"));
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Consumer<Throwable>() {
                        final AtomicInteger subscribeCounter = new AtomicInteger(0);
                        int TOLERABLE_RETRY_TIMES = 5;
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            int subscribeCount = subscribeCounter.incrementAndGet();
                            Timber.i("doOnError:%d", subscribeCount);
                            if(subscribeCount > TOLERABLE_RETRY_TIMES) {
                                mBinding.cancelButton.setVisibility(View.VISIBLE);
                            }
                        }
                    })
                    /*
                    .retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(@NonNull final Observable<Throwable> throwableObservable) throws Exception {
                            return throwableObservable.zipWith(Observable.range(1, 10), new BiFunction<Throwable, Integer, Object>() {
                                @Override
                                public Object apply(@NonNull Throwable throwable, @NonNull Integer retryCount) throws Exception {
                                    if(throwable instanceof RuntimeException) {
                                        if(retryCount == 8) {
                                            Timber.i("throwable");
                                            return throwable;
                                        }
                                        return retryCount;
                                    } else {
                                        return throwable;
                                    }
                                }
                            }).flatMap(new Function<Object, ObservableSource<?>>() {
                                private long INIT_INTERVAL = 500L;
                                private long MAX_RETRY_INTERVAL = 10 * 1000L;
                                @Override
                                public ObservableSource<?> apply(@NonNull Object obj) throws Exception {
                                    if(obj instanceof Integer) {
                                        Integer retryCount = (Integer) obj;
                                        int multiplier = (int)(Math.random() * (int)(Math.pow(2.0, (double) retryCount) - 1));
                                        long retryInterval = multiplier * INIT_INTERVAL;
                                        if (retryInterval > MAX_RETRY_INTERVAL) {
                                            retryInterval = MAX_RETRY_INTERVAL;
                                        }
                                        Timber.i("RuntimeException, retries(%d) with sleep(%d) milliseconds", retryCount, retryInterval);
                                        return Observable.timer(retryInterval, TimeUnit.MILLISECONDS);
                                    } else {
                                        Timber.i("apply throwable");
                                        return Observable.error((Throwable)obj);
                                    }
                                }
                            });
                        }
                    })
                    */
                    .observeOn(Schedulers.computation())
                    .retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
                        //https://en.wikipedia.org/wiki/Exponential_backoff
                        final AtomicInteger retryCounter = new AtomicInteger(0);
                        long INIT_INTERVAL = 500L;
                        long MAX_RETRY_INTERVAL = 10 * 1000L;
                        int MAX_RETRY_TIMES = 10;
                        @Override
                        public ObservableSource<?> apply(@NonNull Observable<Throwable> throwableObservable) throws Exception {
                            return throwableObservable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                                @Override
                                public ObservableSource<?> apply(@NonNull Throwable throwable) throws Exception {
                                    //Only retry when it's RuntimeException
                                    if (throwable instanceof RuntimeException) {
                                        int retries = retryCounter.incrementAndGet();
                                        int multiplier = (int)(Math.random() * (int)(Math.pow(2.0, (double) retries) - 1));
                                        long retryInterval = multiplier * INIT_INTERVAL;
                                        if (retryInterval > MAX_RETRY_INTERVAL) {
                                            retryInterval = MAX_RETRY_INTERVAL;
                                        }

                                        if (retries < MAX_RETRY_TIMES) {
                                            Timber.i("RuntimeException, retries(%d) with sleep(%d) milliseconds", retries, retryInterval);
                                            return Observable.timer(retryInterval, TimeUnit.MILLISECONDS);
                                        }

                                    }
                                    return Observable.error(throwable);
                                }
                            });
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Integer>() {

                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            Timber.i("onSubscribe");
                            mDispose = d;
                        }

                        @Override
                        public void onNext(@NonNull Integer integer) {

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Timber.i("onError");
                            showProgress(false);
                            isLoging = false;
                        }

                        @Override
                        public void onComplete() {
                            Timber.i("onComplete");
                            showProgress(false);
                            isLoging = false;
                        }
                    });
        }
    }

    private boolean isUsernameValid(String email) {
        //TODO: Replace this with your own logic
        return true;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 1;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mBinding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
            mBinding.loginForm
                    .animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBinding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });

            mBinding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            mBinding.loginProgress
                    .animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBinding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mBinding.loginProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            mBinding.loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }

        if(!show) {
            mBinding.cancelButton.setVisibility(View.GONE);
        }

    }

    /**
     * Cancels the upstream's disposable.
     */
    private final void dispose() {
        if (mDispose != null) {
            if (!mDispose.isDisposed()) {
                mDispose.dispose();
            }
        }
    }

}
