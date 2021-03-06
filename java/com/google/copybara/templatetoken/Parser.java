/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.templatetoken;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.events.Location;
import com.google.devtools.build.lib.syntax.EvalException;
import javax.annotation.Nullable;

/**
 * Parse strings like "foo${bar}baz" in a series of literal and interpolation variables
 */
public class Parser {
  @Nullable private final Location location;

  public Parser(@Nullable Location location) {
    this.location = location;
  }

  /**
   * Parses a template. In the raw string representation, interpolation is
   * done with {@code ${var_name}}. Literal dollar signs can be represented with {@code $$}.
   *
   * @throws EvalException if the template is malformed
   */
  public ImmutableList<Token> parse(String template) throws EvalException {
    ImmutableList.Builder<Token> result = ImmutableList.builder();
    StringBuilder currentLiteral = new StringBuilder();
    int c = 0;
    while (c < template.length()) {
      char thisChar = template.charAt(c);
      c++;
      if (thisChar != '$') {
        currentLiteral.append(thisChar);
        continue;
      }
      if (c >= template.length()) {
        throw new EvalException(location, "Expect $ or { after every $ in string: " + template);
      }
      thisChar = template.charAt(c);
      c++;
      switch (thisChar) {
        case '$':
          currentLiteral.append('$');
          break;
        case '{':
          result.add(new Token(currentLiteral.toString(), Token.TokenType.LITERAL));
          currentLiteral = new StringBuilder();
          int terminating = template.indexOf('}', c);
          if (terminating == -1) {
            throw new EvalException(location, "Unterminated '${'. Expected '}': " + template);
          }
          if (c == terminating) {
            throw new EvalException(
                location, "Expect non-empty interpolated value name: " + template);
          }
          result.add(
              new Token(template.substring(c, terminating), Token.TokenType.INTERPOLATION));
          c = terminating + 1;
          break;
        default:
          throw new EvalException(location, "Expect $ or { after every $ in string: " + template);
      }
    }
    result.add(new Token(currentLiteral.toString(), Token.TokenType.LITERAL));
    return result.build();
  }
}
