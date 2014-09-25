/* Copyright 2012-2013 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.cache

import grails.plugin.cache.ehcache.GrailsEhcacheCache
import grails.test.mixin.TestMixin;
import grails.test.mixin.integration.IntegrationTestMixin;
import net.sf.ehcache.config.CacheConfiguration
import net.sf.ehcache.config.Configuration
import org.junit.After
import org.junit.Before
import org.springframework.cache.Cache

import static org.junit.Assert.*

/**
 * @author Burt Beckwith
 */
@TestMixin(IntegrationTestMixin)
class ConfigLoaderTests {

	def grailsApplication
	def grailsCacheConfigLoader
	def grailsCacheManager

	void testConfigClasses() {
		def configClasses = grailsApplication.cacheConfigClasses
		assertEquals 3, configClasses.length
		assertTrue configClasses.clazz.name.contains('DefaultCacheConfig')
		assertTrue configClasses.clazz.name.contains('Order500CacheConfig')
		assertTrue configClasses.clazz.name.contains('Order1500CacheConfig')
	}

	void testLoadConfigs() {

		grailsCacheConfigLoader.reload grailsApplication.mainContext

		assertEquals(['grailsBlocksCache', 'grailsTemplatesCache', 'mycache', 'the_cache'],
		             grailsCacheManager.cacheNames.sort())

		// simulate editing Config.groovy
		grailsApplication.config.grails.cache.config = {
			cache {
				name 'mycache2'
				eternal false
				overflowToDisk true
				maxBytesLocalHeap "20M"
			}

			defaults {
				timeToLiveSeconds 1234
			}
		}

		grailsCacheConfigLoader.reload grailsApplication.mainContext

		assertEquals(['grailsBlocksCache', 'grailsTemplatesCache', 'mycache2', 'the_cache'],
		             grailsCacheManager.cacheNames.sort())
	}

    void testSizeOfPolicy() {
        grailsCacheConfigLoader.reload grailsApplication.mainContext

        // simulate editing Config.groovy
        grailsApplication.config.grails.cache.config = {
            sizeOfPolicy {
                maxDepth 327
                maxDepthExceededBehavior 'abort'
            }
        }

        grailsCacheConfigLoader.reload grailsApplication.mainContext

        Configuration cacheManagerConfiguration = grailsCacheManager.underlyingCacheManager.configuration

        assertNotNull cacheManagerConfiguration.sizeOfPolicyConfiguration
        assertEquals 327,cacheManagerConfiguration.sizeOfPolicyConfiguration.maxDepth
        assertTrue cacheManagerConfiguration.sizeOfPolicyConfiguration.maxDepthExceededBehavior.abort
    }

	void testOrder() {

		grailsCacheConfigLoader.reload grailsApplication.mainContext

		String name = 'the_cache'

		assertTrue grailsCacheManager.cacheExists(name)

		Cache cache = grailsCacheManager.getCache(name)

		org.junit.Assert.assertThat(cache, org.hamcrest.CoreMatchers.instanceOf(GrailsEhcacheCache.class));

		org.junit.Assert.assertThat(cache.nativeCache, org.hamcrest.CoreMatchers.instanceOf(net.sf.ehcache.Ehcache.class));
		CacheConfiguration configuration = cache.nativeCache.cacheConfiguration

		assertTrue configuration.overflowToDisk
		assertEquals "20M", configuration.maxBytesLocalHeapInput
		assertEquals 3, configuration.maxElementsOnDisk
		assertFalse configuration.eternal
		assertEquals 1234, configuration.timeToLiveSeconds
	}

	@Before
	public void setUp() {
		reset()
	}

	@After
	public void tearDown() {
		reset()
	}

	private void clearCaches() {
		// make copy of names to avoid CME
		for (String name in ([] + grailsCacheManager.cacheNames)) {
			assertTrue grailsCacheManager.destroyCache(name)
		}
	}

	private void reset() {

		clearCaches()

		grailsApplication.config.grails.cache.config = {
			cache {
				name 'mycache'
				eternal false
				overflowToDisk true
				maxBytesLocalHeap "10M"
				maxElementsOnDisk 10000000
			}

			defaults {
				timeToLiveSeconds 1234
			}
		}
	}
}
