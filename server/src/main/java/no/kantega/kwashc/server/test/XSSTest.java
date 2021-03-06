/*
 * Copyright 2013 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.kwashc.server.test;

import net.sourceforge.jwebunit.junit.WebTester;
import no.kantega.kwashc.server.model.Site;
import no.kantega.kwashc.server.model.TestResult;

/**
 * Test if various XSS vulnerabilities are present. This test is a bit fragile and can give false negatives if the JS is
 * rewritten without fixing the vulnerabilities.
 *
 * Solution: Remove or fix the offending JS calls:
 * 1) The name parameter is URI decoded, and then written to the DOM as html. Fix either.
 * 2) Never call setTimeout() with user supplied data! The function calls eval(), which will execute any JS, even if escaped!
 *
 * @author Jon Are Rakvaag (Politiets IKT-tjenester)
 */
public class XSSTest extends AbstractTest {
	
    @Override
    public String getName() {
        return "XSS test";
    }

    @Override
    public String getDescription() {
        return "Tests if the site is vulnerable for various XSS vulnerabilities.";
    }

	@Override
	public String getInformationURL() {
		return "https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet";
	}

	@Override
    protected TestResult testSite(Site site, TestResult testResult) throws Throwable {

		WebTester tester = new WebTester();
		tester.beginAt(site.getAddress());
		tester.assertTextPresent("Posts");

		String source = tester.getPageSource();
		boolean isNameVulnerable = isNameVulnerable(source);
		boolean isTimeoutVulnerable = isTimeoutVulnerable(source);
		//the jQuery $(document.hash) vulnerability is covered by the KnownVulnerableComponentsTest.

		if(isNameVulnerable && isTimeoutVulnerable){
			testResult.setPassed(false);
			testResult.setMessage("It's possible to create arbitrary html elements using the name and timeout parameters. " +
					"Try clicking <a href=\"" + site.getAddress() + "blog?name=<img src=x onerror=alert(1)>\">here</a> " +
					"or <a href=\"" + site.getAddress() + "blog?timeout=alert(2)\">here</a>. Some browsers will filter " +
					"the attacks. Try Firefox.");
		}
		else if(isNameVulnerable) {
			testResult.setPassed(false);
			testResult.setMessage("It's possible to create arbitrary html elements using the name parameter. " +
					"Try clicking <a href=\"" + site.getAddress() +	"blog?name=<img src=x onerror=alert(1)>\">here</a>");
		} else if(isTimeoutVulnerable){
			testResult.setMessage("It's possible to create arbitrary html elements using the timeout parameter. " +
					"Try clicking <a href=\"" + site.getAddress() +	"blog?timeout=alert(2)\">here</a>");
		} else{
			testResult.setPassed(true);
			testResult.setMessage("No errors, we hope. This test is a bit fragile, since we can't execute JavaScript in the test framework.");
		}
		return testResult;
    }

	private boolean isTimeoutVulnerable(String source) {
		String[] vulnerableStrings = {"timeout = getParameter(\"timeout\"", "setTimeout(timeout)"};
		return sourceContainsAllStrings(source, vulnerableStrings);
	}

	private boolean isNameVulnerable(String source){
		String[] vulnerableStrings = {"name = getParameter(\"name\")", "document.write(name", "return decodeURIComponent(name"};
		return sourceContainsAllStrings(source, vulnerableStrings);
	}

	private boolean sourceContainsAllStrings(String source, String[] strings){
		for(String string : strings){
			if(!source.contains(string)) {
				return false;
			}
		}
		return true;
	}
}
