package io.github.sheepdestroyer.materialisheep;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.os.BundleCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class BundleCompatTest {

    interface MyInterface extends Parcelable {
        String getValue();
    }

    static class MyObject implements MyInterface {
        private String value;

        public MyObject(String value) {
            this.value = value;
        }

        protected MyObject(Parcel in) {
            value = in.readString();
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(value);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<MyObject> CREATOR = new Creator<MyObject>() {
            @Override
            public MyObject createFromParcel(Parcel in) {
                return new MyObject(in);
            }

            @Override
            public MyObject[] newArray(int size) {
                return new MyObject[size];
            }
        };
    }

    @Test
    public void testGetParcelableWithInterfaceClass() {
        MyObject original = new MyObject("test");
        Bundle bundle = new Bundle();
        bundle.putParcelable("key", original);

        // Retrieve using the interface class
        MyInterface retrieved = BundleCompat.getParcelable(bundle, "key", MyInterface.class);

        assertNotNull(retrieved);
        assertTrue(retrieved instanceof MyObject);
        assertEquals("test", retrieved.getValue());
    }

    @Test
    public void testGetParcelableWithConcreteClass() {
        MyObject original = new MyObject("test");
        Bundle bundle = new Bundle();
        bundle.putParcelable("key", original);

        // Retrieve using the concrete class
        MyObject retrieved = BundleCompat.getParcelable(bundle, "key", MyObject.class);

        assertNotNull(retrieved);
        assertEquals("test", retrieved.getValue());
    }
}
