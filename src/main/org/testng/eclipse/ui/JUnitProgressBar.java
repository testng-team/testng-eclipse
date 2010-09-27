/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.ui;


import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.testng.ITestResult;

/**
 * A progress bar with a red/green indication for success or failure.
 */
public class JUnitProgressBar extends Canvas {
  private static final int DEFAULT_WIDTH = 160;
  private static final int DEFAULT_HEIGHT = 16;

  private int fCurrentTickCount = 0;
  private int fMaxTickCount = 0;
  private int fColorBarWidth = 0;
  private Color fOKColor;
  private Color fFailureColor;
  private Color fStoppedColor;
  private Color fSkippedColor;
  private Color m_messageColor;
  // Defined in ITestResult
  private int fError;
  private boolean fStopped = false;

  private int m_totalTestsCounter;
  private int m_testCounter;
  private int m_totalMethodsCounter;
  private int m_methodsCounter;
  private String m_currentMessage = "Tests: 0/0  Methods: 0/0";
  private String m_timeMessage= "";

  public JUnitProgressBar(Composite parent) {
    super(parent, SWT.NONE);

    addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        fColorBarWidth = scale(fCurrentTickCount);
        redraw();
      }
    });
    addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        paint(e);
      }
    });
    addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        fFailureColor.dispose();
        fOKColor.dispose();
        fStoppedColor.dispose();
      }
    });

    Display display = parent.getDisplay();
    fFailureColor = new Color(display, 159, 63, 63);
    fOKColor = new Color(display, 95, 191, 95);
    fSkippedColor = new Color(display, 255, 193, 37);
    fStoppedColor = new Color(display, 120, 120, 120);
    m_messageColor = display.getSystemColor(SWT.COLOR_BLACK);
  }

  public void setMaximum(int max, int totalMethods) {
//    ppp("setMaximum:[" + fMaxTickCount + "," + fColorBarWidth + "," + max + "," + totalMethods + "]");
    fMaxTickCount = max;

    fColorBarWidth = scale(fCurrentTickCount);
//    ppp("setMaximum:rescaled:" + fColorBarWidth);

    m_totalMethodsCounter = totalMethods;
    paintStep(1, fColorBarWidth);
  }

  public void reset(int testcounter) {
    fError = ITestResult.FAILURE;
    fStopped = false;
    fCurrentTickCount = 0;
    fColorBarWidth = 0;
    fMaxTickCount = 0;
    m_totalTestsCounter = testcounter;
    m_testCounter = 0;
    m_totalMethodsCounter = 0;
    m_methodsCounter = 0;
    m_timeMessage= "";
    m_currentMessage = getCurrentMessage();

    redraw();
//    ppp("reset");
  }

  private String getCurrentMessage() {
    return "Tests: " + m_testCounter + "/" + m_totalTestsCounter + "  Methods: " + m_methodsCounter
      + "/" + m_totalMethodsCounter + m_timeMessage;
  }

  private void paintStep(int startX, int endX) {
    GC gc = new GC(this);
    setStatusColor(gc);
    Rectangle rect = getClientArea();
    startX = Math.max(1, startX);
    gc.fillRectangle(startX, 1, endX - startX, rect.height - 2);
    String string = getCurrentMessage();
    m_currentMessage = string;
    gc.setFont(JFaceResources.getDefaultFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    int stringWidth = fontMetrics.getAverageCharWidth() * string.length();
    int stringHeight = fontMetrics.getHeight();
    gc.setForeground(m_messageColor);
    gc.drawString(string, (rect.width - stringWidth) / 2, (rect.height - stringHeight) / 2, true);

    gc.dispose();
  }

  private void setStatusColor(GC gc) {
    if (fStopped) {
      gc.setBackground(fStoppedColor);
    }
    else if (fError == ITestResult.FAILURE || fError == ITestResult.SUCCESS_PERCENTAGE_FAILURE) {
      gc.setBackground(fFailureColor);
    }
    else if (fError == ITestResult.SKIP) {
      gc.setBackground(fSkippedColor);
    }
    else {
      gc.setBackground(fOKColor);
    }
  }

  public void stopped() {
    fStopped = true;
    redraw();
  }

  private int scale(int value) {
    if (fMaxTickCount > 0) {
      Rectangle r = getClientArea();
//      ppp("scale:[" + r + "][" + value + "][" + fMaxTickCount + "]");
      if (r.width != 0) {
        return Math.max(0, value * (r.width - 2) / fMaxTickCount);
      }
    }

    return value;
  }

  private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topleft, Color bottomright) {
    gc.setForeground(topleft);
    gc.drawLine(x, y, x + w - 1, y);
    gc.drawLine(x, y, x, y + h - 1);

    gc.setForeground(bottomright);
    gc.drawLine(x + w, y, x + w, y + h);
    gc.drawLine(x, y + h, x + w, y + h);
  }

  private void paint(PaintEvent event) {
    GC gc = event.gc;
    Display disp = getDisplay();

    Rectangle rect = getClientArea();
    gc.fillRectangle(rect);
    drawBevelRect(gc,
                  rect.x,
                  rect.y,
                  rect.width - 1,
                  rect.height - 1,
                  disp.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW),
                  disp.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

    setStatusColor(gc);
    fColorBarWidth = Math.min(rect.width - 2, fColorBarWidth);
    gc.fillRectangle(1, 1, fColorBarWidth, rect.height - 2);

    gc.setFont(JFaceResources.getDefaultFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    final String msg= getCurrentMessage();
    int stringWidth = fontMetrics.getAverageCharWidth() * msg.length();
    int stringHeight = fontMetrics.getHeight();
    gc.setForeground(m_messageColor);
    gc.drawString(msg,
                  (rect.width - stringWidth) / 2,
                  (rect.height - stringHeight) / 2,
                  true);
  }

  public Point computeSize(int wHint, int hHint, boolean changed) {
    checkWidget();
    Point size = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    if (wHint != SWT.DEFAULT) {
      size.x = wHint;
    }
    if (hHint != SWT.DEFAULT) {
      size.y = hHint;
    }

    return size;
  }

  public void step(int failures) {
    fCurrentTickCount++;
    m_methodsCounter++;
    int x = fColorBarWidth;

    fColorBarWidth = scale(fCurrentTickCount);
    if (fError == ITestResult.SUCCESS && (failures > 0)) {
      fError = ITestResult.FAILURE;
      x = 1;
    }
    if (fCurrentTickCount == fMaxTickCount) {
      fColorBarWidth = getClientArea().width - 1;
    }
    paintStep(x, fColorBarWidth);
  }

  public void stepTests() {
    m_testCounter++;
    m_currentMessage = getCurrentMessage();
    redraw();
  }

  public void refresh(int status, String msg) {
    fError = status;
    m_timeMessage= msg;
    redraw();
  }

  private static void ppp(Object msg) {
//    System.out.println("[JUP]: " + msg);
  }
}
