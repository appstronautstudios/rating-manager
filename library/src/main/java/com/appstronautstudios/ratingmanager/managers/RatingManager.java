package com.appstronautstudios.ratingmanager.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.appstronautstudios.ratingmanager.R;
import com.appstronautstudios.ratingmanager.utils.YesNoCancelListener;
import com.codemybrainsout.ratingdialog.RatingDialog;
import com.kobakei.ratethisapp.RateThisApp;

import java.util.concurrent.TimeUnit;

public class RatingManager {

    private static final RatingManager INSTANCE = new RatingManager();

    private RatingManager() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    public static RatingManager getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        int sessions = getNumberOfSessions(context);
        sessions++;

        setNumberOfSessions(context, sessions);
    }

    public void showRTA(final Context context, YesNoCancelListener listener) {
        showRTA(context, 0, 0, true, listener);
    }

    public void showRTA(final Context context, long msInterval, int sessionInterval, boolean force, final YesNoCancelListener listener) {
        if (getLastTimeRtaShown(context) == -1) {
            // never been shown before. Consider this the starting point of last shown tracking
            // so that the time diff is in days not years since 1970
            setLastTimeRtaShown(context, System.currentTimeMillis());
        }

        RateThisApp.init(new RateThisApp.Config(0, 0));
        RateThisApp.setCallback(new RateThisApp.Callback() {
            @Override
            public void onYesClicked() {
                if (listener != null) {
                    listener.onYes();
                }
            }

            @Override
            public void onNoClicked() {
                if (listener != null) {
                    listener.onNo();
                }
            }

            @Override
            public void onCancelClicked() {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
        RateThisApp.onCreate(context);

        if (force) {
            // force show regardless of any restrictions
            RateThisApp.showRateDialog(context);
            setLastTimeRtaShown(context, System.currentTimeMillis());
        } else if (System.currentTimeMillis() - getLastTimeRtaShown(context) > msInterval) {
            // interval passed. Show rate dialogue IF NEEDED. This internally checks if the user
            // has either voted or said never show again. 0 day and 0 launch time criteria will
            // always pass.
            RateThisApp.showRateDialogIfNeeded(context);
            setLastTimeRtaShown(context, System.currentTimeMillis());
        } else if (getNumberOfSessions(context) >= sessionInterval) {
            // session counter passed. Show every time
            RateThisApp.showRateDialogIfNeeded(context);
            setLastTimeRtaShown(context, System.currentTimeMillis());
        }
    }

    public void showRAB(final Context context, int threshold, String email, YesNoCancelListener listener) {
        showRAB(context, 0, threshold, email, true, listener);
    }

    public void showRAB(final Context context, int sessionInterval, int threshold, final String email, boolean force, final YesNoCancelListener listener) {
        RatingDialog.Builder.RatingThresholdClearedListener clearedListener = new RatingDialog.Builder.RatingThresholdClearedListener() {
            @Override
            public void onThresholdCleared(@NonNull RatingDialog ratingDialog, float rating, boolean thresholdCleared) {
                ratingDialog.dismiss();
                new AlertDialog.Builder(context)
                        .setTitle("Rate our app")
                        .setMessage("If you enjoy using this app, please take a moment to leave a rating on the Play Store.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    listener.onYes();
                                }

                                final String appPackageName = context.getPackageName(); // getPackageName() from Context or Activity object
                                try {
                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                }
                            }
                        })
                        .setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (listener != null) {
                                    listener.onNo();
                                }
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (listener != null) {
                                    listener.onCancel();
                                }
                            }
                        })
                        .create()
                        .show();
            }
        };

        RatingDialog.Builder.RatingThresholdFailedListener failedListener = new RatingDialog.Builder.RatingThresholdFailedListener() {
            @Override
            public void onThresholdFailed(@NonNull RatingDialog ratingDialog, float rating, boolean thresholdCleared) {
                ratingDialog.dismiss();
                new AlertDialog.Builder(context)
                        .setTitle("How can we improve?")
                        .setMessage("Your feedback is important to us. Would you mind sending us a quick email to let us know what parts of this app you would like to see improved?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (listener != null) {
                                    listener.onYes();
                                }

                                final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                                context.startActivity(emailIntent);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (listener != null) {
                                    listener.onNo();
                                }
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (listener != null) {
                                    listener.onCancel();
                                }
                            }
                        })
                        .create()
                        .show();
            }
        };

        RatingDialog ratingDialog = new RatingDialog.Builder(context)
                .threshold(threshold)
                .onThresholdCleared(clearedListener)
                .onThresholdFailed(failedListener)
                .build();

        if (force) {
            // force show regardless of any restrictions
            ratingDialog.show();
        } else if (getNumberOfSessions(context) >= sessionInterval) {
            // session counter passed. Show every time
            ratingDialog.show();
        }
    }

    public long getDaysSinceInstall(@NonNull Context context) {
        long installTs = System.currentTimeMillis();
        PackageManager packMan = context.getPackageManager();
        try {
            PackageInfo pkgInfo = packMan.getPackageInfo(context.getPackageName(), 0);
            installTs = pkgInfo.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installTs);
    }

    private void setLastTimeRtaShown(Context context, long ts) {
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferenceManager.edit();
        editor.putLong(context.getString(R.string.key_rm_last), ts);
        editor.apply();
    }

    public long getLastTimeRtaShown(Context context) {
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        return preferenceManager.getLong(context.getString(R.string.key_rm_last), -1);
    }

    private void setNumberOfSessions(Context context, int sessions) {
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferenceManager.edit();
        editor.putInt(context.getString(R.string.key_rm_sessions), sessions);
        editor.apply();
    }

    public int getNumberOfSessions(Context context) {
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        return preferenceManager.getInt(context.getString(R.string.key_rm_sessions), 0);
    }
}
