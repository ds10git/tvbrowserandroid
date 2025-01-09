package org.tvbrowser.tvbrowser;

import android.content.Context;
import android.os.Build;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

public final class NetHelper {
    public static void prepareConnection(Context context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            try {
                ProviderInstaller.installIfNeeded(context);
            } catch (GooglePlayServicesRepairableException e) {
                throw new RuntimeException(e);
            } catch (GooglePlayServicesNotAvailableException e) {
                throw new RuntimeException(e);
            }
            SSLTool.disableCertificateValidation();
        }
    }

    public static void finishConnection() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            SSLTool.resetCertificateValidation();
        }
    }
}
