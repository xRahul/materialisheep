/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep.widget;

import android.annotation.SuppressLint;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public abstract class LinkTouchListener implements View.OnTouchListener {
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!(v instanceof TextView)) {
            return false;
        }
        TextView widget = (TextView) v;
        CharSequence text = widget.getText();
        if (!(text instanceof Spanned)) {
            return false;
        }
        Spannable spannable = text instanceof Spannable ? (Spannable) text : null;

        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            if (layout == null) {
                return false;
            }
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            // Ghost link check
            if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) {
                if (action == MotionEvent.ACTION_UP && spannable != null) {
                    Selection.removeSelection(spannable);
                }
                return false;
            }

            ClickableSpan[] links = ((Spanned) text).getSpans(off, off, ClickableSpan.class);

            if (links.length != 0) {
                ClickableSpan link = links[0];
                if (action == MotionEvent.ACTION_UP) {
                    if (spannable != null) {
                        Selection.removeSelection(spannable);
                    }
                    onLinkClick(widget, link);
                } else { // DOWN
                    if (spannable != null) {
                        Selection.setSelection(spannable,
                                ((Spanned) text).getSpanStart(link),
                                ((Spanned) text).getSpanEnd(link));
                    }
                }
                return true;
            } else {
                if (spannable != null) {
                    Selection.removeSelection(spannable);
                }
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            if (spannable != null) {
                Selection.removeSelection(spannable);
            }
        }

        return false;
    }

    public abstract void onLinkClick(TextView widget, ClickableSpan span);
}
