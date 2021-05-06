/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * A string pattern matcher. Supports '*' and '?' wildcards.
 *
 * @since 3.12
 */
public final class StringMatcher {

  private final String fPattern;

  private final int fLength; // pattern length

  private final boolean fIgnoreCase;

  private boolean fIgnoreWildCards;

  private boolean fHasLeadingStar;

  private boolean fHasTrailingStar;

  private String fSegments[]; // the given pattern is split into * separated segments

  /* Minimum length required for a match: shorter texts cannot possibly match. */
  private int fBound = 0;

  private static final char fSingleWildCard = '\u0000';

  /**
   * Start and end positions of a shortest match found by
   * {@link StringMatcher#find(String, int, int)}.
   * <p>
   * Note that {@link StringMatcher#find(String, int, int) find()} returns
   * {@code null} if there is no match, so the start and end indices are always
   * non-negative.
   * </p>
   */
  public static final class Position {

    private final int start; // inclusive
    private final int end; // exclusive

    /**
     * Creates a new {@link Position}.
     *
     * @param start index of first matched character
     * @param end   index after the last matched character
     */
    public Position(int start, int end) {
      this.start = start;
      this.end = end;
    }

    /**
     * Retrieves the index of the first matched character.
     *
     * @return the index of the first matched character
     */
    public int getStart() {
      return start;
    }

    /**
     * Retrieves the index after the last matched character.
     *
     * @return the index after the last matched character
     */
    public int getEnd() {
      return end;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + end;
      result = prime * result + start;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Position other = (Position) obj;
      return end == other.end && start == other.start;
    }

    @Override
    public String toString() {
      return "Position(" + start + ',' + end + ')'; //$NON-NLS-1$
    }
  }

  /**
   * StringMatcher constructor takes in a String object that is a simple pattern.
   * The pattern may contain '*' for 0 and many characters and '?' for exactly one
   * character.
   * <p>
   * Literal '*' and '?' characters must be escaped in the pattern e.g., "\*"
   * means literal "*", etc.
   * </p>
   * <p>
   * The escape character '\' is an escape only if followed by '*', '?', or '\'.
   * All other occurrences are taken literally.
   * </p>
   * <p>
   * If invoking the StringMatcher with string literals in Java, don't forget
   * escape characters are represented by "\\".
   * </p>
   * <p
   * An empty pattern matches only an empty text, unless {@link #usePrefixMatch()}
   * is used, in which case it always matches.
   * </p>
   *
   * @param pattern         the pattern to match text against, must not be {@code null}
   * @param ignoreCase      if true, case is ignored
   * @param ignoreWildCards if true, wild cards and their escape sequences are
   *                        ignored (everything is taken literally).
   * @throws IllegalArgumentException if {@code pattern == null}
   */
  public StringMatcher(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
    if (pattern == null) {
      throw new IllegalArgumentException();
    }
    fIgnoreCase = ignoreCase;
    fIgnoreWildCards = ignoreWildCards;
    fPattern = pattern;
    fLength = pattern.length();

    if (fIgnoreWildCards) {
      parseNoWildCards();
    } else {
      parseWildCards();
    }
  }

  /**
   * Configures this {@link StringMatcher} to also match on prefix-only matches.
   * <p>
   * If the matcher was created with {@code ignoreWildCards == true}, any wildcard
   * characters in the pattern will still be matched literally.
   * </p>
   * <p>
   * If the pattern is empty, it will match any text.
   * </p>
   *
   * @since 3.13
   */
  public void usePrefixMatch() {
    fIgnoreWildCards = false;
    fHasTrailingStar = true;
  }

  /**
   * Finds the first occurrence of the pattern between {@code start} (inclusive)
   * and {@code end} (exclusive).
   * <p>
   * If wildcards are enabled: If the pattern contains only '*' wildcards a full
   * match is reported, otherwise leading and trailing '*' wildcards are ignored.
   * If the pattern contains interior '*' wildcards, the first <em>shortest</em>
   * match is returned.
   * </p>
   *
   * @param text  the String object to search in; must not be {@code null}
   * @param start the starting index of the search range, inclusive
   * @param end   the ending index of the search range, exclusive
   * @return a {@link Position} object for the match found, or {@code null} if
   *         there's no match or the text range to search is empty (end &lt;=
   *         start). If the pattern is empty, the position will describe a
   *         null-match in the {@code text} at {@code start}
   *         ({@link Position#getStart() getStart()} == {@link Position#getEnd()
   *         getEnd()} == {@code start}).<br/>
   *         <b>Note:</b> for patterns like "*abc*" with leading and trailing
   *         stars, the position of "abc" is returned. For a pattern like"*??*" in
   *         text "abcdf", (0,2) is returned. Interior '*'s yield the
   *         <em>shortest</em> match: for pattern "a*b" and text "axbyb", (0,3) is
   *         returned, not (0,5).
   * @throws IllegalArgumentException if {@code text == null}
   */
  public Position find(String text, int start, int end) {
    if (text == null) {
      throw new IllegalArgumentException();
    }
    int tlen = text.length();
    if (start < 0) {
      start = 0;
    }
    if (end > tlen) {
      end = tlen;
    }
    if (end < 0 || start >= end) {
      return null;
    }
    if (fLength == 0) {
      return new Position(start, start);
    }
    if (fIgnoreWildCards) {
      int x = textPosIn(text, start, end, fPattern);
      return x < 0 ? null : new Position(x, x + fLength);
    }

    int segCount = fSegments.length;
    if (segCount == 0) {
      // Pattern contains only '*'(s)
      return new Position(start, end);
    }

    int curPos = start;
    int matchStart = -1;
    int i;
    for (i = 0; i < segCount && curPos < end; ++i) {
      String current = fSegments[i];
      int nextMatch = regExpPosIn(text, curPos, end, current);
      if (nextMatch < 0) {
        return null;
      }
      if (i == 0) {
        matchStart = nextMatch;
      }
      curPos = nextMatch + current.length();
    }
    return i < segCount ? null : new Position(matchStart, curPos);
  }

  /**
   * Determines whether the given {@code text} matches the pattern.
   *
   * @param text String to match; must not be {@code null}
   * @return {@code true} if the whole {@code text} matches the pattern;
   *         {@code false} otherwise
   * @throws IllegalArgumentException if {@code text == null}
   */
  public boolean match(String text) {
    if (text == null) {
      throw new IllegalArgumentException();
    }
    return match(text, 0, text.length());
  }

  /**
   * Determines whether the given sub-string of {@code text} from {@code start}
   * (inclusive) to {@code end} (exclusive) matches the pattern.
   *
   * @param text  String to match in; must not be {@code null}
   * @param start start index (inclusive) within {@code text} of the sub-string to
   *              match
   * @param end   end index (exclusive) within {@code text} of the sub-string to
   *              match
   * @return {@code true} if the given slice of {@code text} matches the pattern;
   *         {@code false} otherwise
   * @throws IllegalArgumentException if {@code text == null}
   */
  public boolean match(String text, int start, int end) {
    if (text == null) {
      throw new IllegalArgumentException();
    }
    if (start > end) {
      return false;
    }
    if (fIgnoreWildCards) {
      return (end - start == fLength) && fPattern.regionMatches(fIgnoreCase, 0, text, start, fLength);
    }

    int segCount = fSegments.length;
    if (segCount == 0 && (fHasLeadingStar || fHasTrailingStar)) {
      // Pattern contains only '*'(s)
      return true;
    }
    if (start == end) {
      return fLength == 0;
    }
    if (fLength == 0) {
      return start == end;
    }

    int tlen = text.length();
    if (start < 0) {
      start = 0;
    }
    if (end > tlen) {
      end = tlen;
    }

    int tCurPos = start;
    int bound = end - fBound;
    if (bound < 0) {
      return false;
    }

    int i = 0;
    String current = fSegments[i];
    int segLength = current.length();

    // Process first segment
    if (!fHasLeadingStar) {
      if (!regExpRegionMatches(text, start, current, segLength)) {
        return false;
      }
      ++i;
      tCurPos = tCurPos + segLength;
    }
    if ((fSegments.length == 1) && (!fHasLeadingStar) && (!fHasTrailingStar)) {
      // Only one segment to match, no wildcards specified
      return tCurPos == end;
    }
    // Process middle segments
    while (i < segCount) {
      current = fSegments[i];
      int currentMatch;
      int k = current.indexOf(fSingleWildCard);
      if (k < 0) {
        currentMatch = textPosIn(text, tCurPos, end, current);
        if (currentMatch < 0) {
          return false;
        }
      } else {
        currentMatch = regExpPosIn(text, tCurPos, end, current);
        if (currentMatch < 0) {
          return false;
        }
      }
      tCurPos = currentMatch + current.length();
      i++;
    }

    // Process final segment
    if (!fHasTrailingStar && tCurPos != end) {
      int clen = current.length();
      return regExpRegionMatches(text, end - clen, current, clen);
    }
    return i == segCount;
  }

  /**
   * Returns the single segment for a matcher ignoring wildcards.
   */
  private void parseNoWildCards() {
    fSegments = new String[] {fPattern};
    fBound = fLength;
  }

  /**
   * Parses the given pattern into segments separated by wildcard '*' characters.
   */
  private void parseWildCards() {
    if (fPattern.startsWith("*")) { //$NON-NLS-1$
      fHasLeadingStar = true;
    }

    List<String> temp = new ArrayList<>();

    int pos = 0;
    StringBuilder buf = new StringBuilder();
    while (pos < fLength) {
      char c = fPattern.charAt(pos++);
      switch (c) {
        case '\\' :
          if (pos >= fLength) {
            // Lone backslash at the end is taken literally
            buf.append(c);
          } else {
            char next = fPattern.charAt(pos++);
            if (next == '*' || next == '?' || next == '\\') {
              // It _is_ an escape sequence
              buf.append(next);
            } else {
              // Not an escape sequence, just insert literally
              buf.append(c);
              buf.append(next);
            }
          }
          break;
        case '*' :
          if (buf.length() > 0) {
            // Create a new segment
            temp.add(buf.toString());
            fBound += buf.length();
            buf.setLength(0);
          }
          if (pos >= fLength) {
            fHasTrailingStar = true;
          }
          break;
        case '?' :
          // Append special character representing single match wildcard
          buf.append(fSingleWildCard);
          break;
        default :
          buf.append(c);
      }
    }

    // Add last buffer to segment list
    if (buf.length() > 0) {
      temp.add(buf.toString());
      fBound += buf.length();
    }

    fSegments = temp.toArray(new String[0]);
  }

  /**
   * Determines the position of the first match of pattern {@code p}, which must
   * not contain wildcards, in the region {@code text[start..end-1]}.
   *
   * @param text  to find the pattern match in
   * @param start the starting index in the text for search, inclusive
   * @param end   the stopping point of search, exclusive
   * @param p     a plain text without any wildcards
   * @return the starting index in the text of the pattern , or -1 if not found
   */
  private int textPosIn(String text, int start, int end, String p) {
    int plen = p.length();
    int max = end - plen;

    if (!fIgnoreCase) {
      int i = text.indexOf(p, start);
      if (i < 0 || i > max) {
        return -1;
      }
      return i;
    }

    for (int i = start; i <= max; ++i) {
      if (text.regionMatches(true, i, p, 0, plen)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Determines the position of the first match of pattern {@code p} in the region
   * {@code text[start..end-1]}.
   *
   * @param text  to find the pattern match in
   * @param start the starting index in the text for search, inclusive
   * @param end   the stopping point of search, exclusive
   * @param p     a simple regular expression that may contain single-character
   *              wildcards
   * @return the starting index in the text of the pattern , or -1 if not found
   */
  private int regExpPosIn(String text, int start, int end, String p) {
    int plen = p.length();
    int max = end - plen;

    for (int i = start; i <= max; ++i) {
      if (regExpRegionMatches(text, i, p, plen)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Determines whether the region {@code text[tstart..tstart+plen-1]} matches the
   * pattern {@code p}, which may contain single-character wildcards.
   *
   * @param text   String to match against the pattern
   * @param tStart Index in {@code text} to start matching at
   * @param p      String pattern to match against; may contain single-character
   *               wildcards
   * @param plen   Length of {@code p}
   * @return {@code true} if the text matches; {@code false} otherwise
   */
  private boolean regExpRegionMatches(String text, int tStart, String p, int plen) {
    int pStart = 0;
    while (plen-- > 0) {
      char pchar = p.charAt(pStart++);
      if (pchar == fSingleWildCard) {
        tStart++;
        continue;
      }
      char tchar = text.charAt(tStart++);
      if (pchar == tchar) {
        continue;
      }
      if (fIgnoreCase) {
        if (Character.toUpperCase(tchar) == Character.toUpperCase(pchar))
          continue;
        // comparing after converting to upper case doesn't handle all cases;
        // also compare after converting to lower case
        if (Character.toLowerCase(tchar) == Character.toLowerCase(pchar))
          continue;
      }
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    String flags = ""; //$NON-NLS-1$
    if (fIgnoreCase) {
      flags += 'i';
    }
    if (fHasTrailingStar) {
      flags += 't';
    }
    if (!fIgnoreWildCards) {
      flags += '*';
    }
    String result = '[' + fPattern;
    if (!flags.isEmpty()) {
      result += '/' + flags;
    }
    return result + ']';
  }
}