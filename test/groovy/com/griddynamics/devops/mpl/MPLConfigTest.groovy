//
// Copyright (c) 2019 Grid Dynamics International, Inc. All Rights Reserved
// https://www.griddynamics.com
//
// Classification level: Public
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Id: $
// @Project:     MPL
// @Description: Shared Jenkins Modular Pipeline Library
//

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatExceptionOfType

import com.griddynamics.devops.mpl.MPLConfig
import com.griddynamics.devops.mpl.MPLException

class MPLConfigTest {
  def CFG = null

  @Before
  void setUp() throws Exception {
    this.CFG = new MPLConfig([
      first1_level: [
        second1_level: [
          third1_value: 5,
          third2_value: 'something1',
          third3_value: [
            'first',
            2,
            [ fourth1_value: 'test-str' ],
          ],
          '25': 'success',
          '-25': 'success too',
        ],
        second2_value: 312,
      ],
      first2_level: [
        second3_value: 'something2',
      ],
      first3_value: 'test-value',
    ])
  }

  void getExistingValues(cfg) {
    // Simple
    assertThat(cfg.first3_value)
      .as('Check get the simple string value on the first level')
      .isEqualTo('test-value')
    assertThat(cfg.'first1_level.second1_level.third1_value')
      .as('Check get the simple number value of nested map')
      .isEqualTo(5)
    assertThat(cfg.'first1_level.second1_level.third2_value')
      .as('Check get the simple string value of nested map')
      .isEqualTo('something1')

    // List
    assertThat(cfg.'first1_level.second1_level.third3_value.1')
      .as('Check get the simple string value of nested list')
      .isEqualTo(2)
    assertThat(cfg.'first1_level.second1_level.third3_value.2.fourth1_value')
      .as('Check get the simple string value of nested list of nested map')
      .isEqualTo('test-str')

    // Map
    assertThat(cfg.'first1_level.second1_level.25')
      .as('Check get the simple string value of nested map with pos int key')
      .isEqualTo('success')
    assertThat(cfg.'first1_level.second1_level.-25')
      .as('Check get the simple string value of nested map with neg int key')
      .isEqualTo('success too')
  }

  void changeExistingValues(cfg) {
    // Simple
    cfg.first3_value = 'super-test-value'
    assertThat(cfg.'first3_value')
      .as('Check modify a simple string value of map')
      .isEqualTo('super-test-value')

    cfg.'first1_level.second1_level.third1_value' = 66
    assertThat(cfg.'first1_level.second1_level.third1_value')
      .as('Check get a simple number value of nested map')
      .isEqualTo(66)

    cfg.'first1_level.second1_level.third2_value' = 'not something'
    assertThat(cfg.'first1_level.second1_level.third2_value')
      .as('Check modify a simple string value of nested map')
      .isEqualTo('not something')

    // List
    cfg.'first1_level.second1_level.third3_value.1' = 22
    assertThat(cfg.'first1_level.second1_level.third3_value.1')
      .as('Check modify a simple number value of nested list')
      .isEqualTo(22)

    cfg.'first1_level.second1_level.third3_value.2.fourth1_value' = 'super-test-str'
    assertThat(cfg.'first1_level.second1_level.third3_value.2.fourth1_value')
      .as('Check modify a simple string value of nested list of nested map')
      .isEqualTo('super-test-str')

    // Map with string numeric keys
    cfg.'first1_level.second1_level.25' = 'better success'
    assertThat(cfg.'first1_level.second1_level.25')
      .as('Check modify a simple string value of nested map with pos int key')
      .isEqualTo('better success')
    cfg.'first1_level.second1_level.-25' = 'better success too'
    assertThat(cfg.'first1_level.second1_level.-25')
      .as('Check modify a simple string value of nested map with neg int key')
      .isEqualTo('better success too')
  }

  @Test
  void test_get_values() throws Exception {
    getExistingValues(CFG)
  }

  @Test
  void test_get_values_clone() throws Exception {
    def CFG2 = CFG.clone()

    getExistingValues(CFG2)
  }

  @Test
  void test_wrong_paths_return_null() throws Exception {
    // Simple
    assertThat(CFG.first1_level_wrong)
      .as('Check get the wrong map key')
      .isNull()
    assertThat(CFG.'first3_level.asd')
      .as('Check get the good map key string with invalid sublevel')
      .isNull()
    assertThat(CFG.'first1_level.second2_value.asd')
      .as('Check get the good map key number with invalid sublevel')
      .isNull()

    // Map
    assertThat(CFG.'first1_level.second1_level_wrong')
      .as('Check get the wrong nested map key')
      .isNull()
    assertThat(CFG.'first1_level.1')
      .as('Check get the wrong nested map int key')
      .isNull()

    // List
    assertThat(CFG.'first1_level.second1_level.third3_value.asd')
      .as('Check get the wrong nested list invalid string key')
      .isNull()
    assertThat(CFG.'first1_level.second1_level.third3_value.-200')
      .as('Check get the wrong nested list invalid negative index')
      .isNull()
    assertThat(CFG.'first1_level.second1_level.third3_value.200')
      .as('Check get the wrong nested list invalid not existing index')
      .isNull()
    assertThat(CFG.'first1_level.second1_level.third3_value.2.asd')
      .as('Check get the wrong nested map on list')
      .isNull()
  }

  @Test
  void test_immutable_config_values() throws Exception {
    def third1_value = CFG.'first1_level.second1_level.third1_value'
    third1_value = 6
    assertThat(third1_value)
      .as('Check local third1_value var was modified')
      .isEqualTo(6)
    assertThat(CFG.'first1_level.second1_level.third1_value')
      .as('Check not modified number value of nested map')
      .isEqualTo(5)

    def third2_value = CFG.'first1_level.second1_level.third2_value'
    third2_value = 'nothing 2'
    assertThat(third2_value)
      .as('Check local third2_value var was modified')
      .isEqualTo('nothing 2')
    assertThat(CFG.'first1_level.second1_level.third2_value')
      .as('Check not modified string value of nested map')
      .isEqualTo('something1')

    def first2_level = CFG.'first2_level'
    first2_level.second3_value = 'asdasd'
    assertThat(first2_level.second3_value)
      .as('Check local first2_level Map var was modified')
      .isEqualTo('asdasd')
    assertThat(CFG.'first2_level.second3_value')
      .as('Check not modified Map value of nested map')
      .isEqualTo('something2')

    def third3_value = CFG.'first1_level.second1_level.third3_value'
    third3_value[1] = 'asdasd'
    assertThat(third3_value[1])
      .as('Check local third3_value List var was modified')
      .isEqualTo('asdasd')
    assertThat(CFG.'first1_level.second1_level.third3_value.1')
      .as('Check not modified List value of nested map')
      .isEqualTo(2)
  }

  @Test
  void test_set_existing_config_values() throws Exception {
    changeExistingValues(CFG)
  }

  @Test
  void test_set_existing_config_values_clone() throws Exception {
    def CFG2 = CFG.clone()

    changeExistingValues(CFG2)
    getExistingValues(CFG) // Original CFG should not be modified
  }

  @Test
  void test_set_invalid_sublevels_for_existing_config_values() throws Exception {
    // Simple
    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first3_value.asd' = 'asd' })
      .withMessage('''Invalid config key path '<first3_value>.asd': marked key value type 'class java.lang.String' is not suitable to set the nested variable''')
    assertThat(CFG.first3_value)
      .as('Check that the simple string was not modified')
      .isEqualTo('test-value')

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second2_value.asd' = 66 })
      .withMessage('''Invalid config key path 'first1_level.<second2_value>.asd': marked key value type 'class java.lang.Integer' is not suitable to set the nested variable''')
    assertThat(CFG.'first1_level.second2_value')
      .as('Check that the simple number was not modified')
      .isEqualTo(312)

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first3_value.asd.asd' = 'asd' })
      .withMessage('''Invalid config key path '<first3_value>.asd.asd': marked key value type 'class java.lang.String' is not suitable to set the nested variable''')
    assertThat(CFG.first3_value)
      .as('Check that the simple string was not modified')
      .isEqualTo('test-value')

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second2_value.asd.asd' = 66 })
      .withMessage('''Invalid config key path 'first1_level.<second2_value>.asd.asd': marked key value type 'class java.lang.Integer' is not suitable to set the nested variable''')
    assertThat(CFG.'first1_level.second2_value')
      .as('Check that the simple number was not modified')
      .isEqualTo(312)

    // List
    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second1_level.third3_value.-31' = 22 })
      .withMessage('''Invalid config key path 'first1_level.second1_level.third3_value.<-31>': marked key of the list 'third3_value' is not a positive integer''')

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second1_level.third3_value.asd' = 22 })
      .withMessage('''Invalid config key path 'first1_level.second1_level.third3_value.<asd>': marked key of the list 'third3_value' is not a positive integer''')

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second1_level.third3_value.-31.asd' = 22 })
      .withMessage('''Invalid config key path 'first1_level.second1_level.third3_value.<-31>.asd': marked key of the list 'third3_value' is not a positive integer''')

    assertThatExceptionOfType(MPLException.class)
      .isThrownBy({ CFG.'first1_level.second1_level.third3_value.asd.asd' = 22 })
      .withMessage('''Invalid config key path 'first1_level.second1_level.third3_value.<asd>.asd': marked key of the list 'third3_value' is not a positive integer''')
  }

  @Test
  void test_append_existing_config_values() throws Exception {
    // Simple
    CFG.first3_value += '-append'
    assertThat(CFG.'first3_value')
      .as('Check append a simple string value of map with string')
      .isEqualTo('test-value-append')
    CFG.first3_value += 3
    assertThat(CFG.'first3_value')
      .as('Check append a simple string value of map with number')
      .isEqualTo('test-value-append3')

    // Map
    CFG.first1_level += [asd: 3]
    assertThat(CFG.'first1_level.asd')
      .as('Check append a map to existing map')
      .isEqualTo(3)

    // List
    CFG.'first1_level.second1_level.third3_value' += [55, 'string_here']
    assertThat(CFG.'first1_level.second1_level.third3_value.3')
      .as('Check append a list to existing list with number')
      .isEqualTo(55)
    assertThat(CFG.'first1_level.second1_level.third3_value.4')
      .as('Check append a list to existing list with string')
      .isEqualTo('string_here')
  }

  @Test
  void test_set_new_config_values_nested_list_map() throws Exception {
    // Simple
    CFG.'first1_level.second1_level.third3_value.2.new_value' = 'new-test-str'
    assertThat(CFG.'first1_level.second1_level.third3_value.2.new_value')
      .as('Check set new nested map key with a simple string value')
      .isEqualTo('new-test-str')

    CFG.'first1_level.second1_level.third3_value.2.3' = 'new-test-str'
    assertThat(CFG.'first1_level.second1_level.third3_value.2.3')
      .as('Check set new nested map key as index with a simple string value')
      .isEqualTo('new-test-str')

    // List
    CFG.'first1_level.second1_level.third3_value.2.asd1' = [1,2,3,4]
    assertThat(CFG.'first1_level.second1_level.third3_value.2.asd1.2')
      .as('Check that new nested map list value is set properly')
      .isEqualTo(3)

    // Map
    CFG.'first1_level.second1_level.third3_value.2.asd2' = [asd:1, dsd:[1,2],'3':3]
    assertThat(CFG.'first1_level.second1_level.third3_value.2.asd2.asd')
      .as('Check that new nested map map value is set properly')
      .isEqualTo(1)
    assertThat(CFG.'first1_level.second1_level.third3_value.2.asd2.dsd.1')
      .as('Check that new nested map map value is set properly')
      .isEqualTo(2)
    assertThat(CFG.'first1_level.second1_level.third3_value.2.asd2.3')
      .as('Check that new nested map map value is set properly')
      .isEqualTo(3)

    // Old values
    assertThat(CFG.'first1_level.second1_level.third3_value.2.fourth1_value')
      .as('Check after modification of the map the existing value is still here')
      .isEqualTo('test-str')
  }

  @Test
  void test_set_new_config_values_nested_list() throws Exception {
    // Simple
    CFG.'first1_level.second1_level.third3_value.35' = 'new-test-str'
    assertThat(CFG.'first1_level.second1_level.third3_value.35')
      .as('Check set new nested list index with a simple string value')
      .isEqualTo('new-test-str')

    CFG.'first1_level.second1_level.third3_value.37' = 44
    assertThat(CFG.'first1_level.second1_level.third3_value.37')
      .as('Check that new nested list number value is set properly')
      .isEqualTo(44)

    // List
    CFG.'first1_level.second1_level.third3_value.38' = [1,2,3,4]
    assertThat(CFG.'first1_level.second1_level.third3_value.38.2')
      .as('Check that new nested list list value is set properly')
      .isEqualTo(3)

    // Map
    CFG.'first1_level.second1_level.third3_value.40' = [asd:1, dsd:[1,2],'3':3]
    assertThat(CFG.'first1_level.second1_level.third3_value.40.asd')
      .as('Check that new nested list map value is set properly')
      .isEqualTo(1)
    assertThat(CFG.'first1_level.second1_level.third3_value.40.dsd.1')
      .as('Check that new nested list map value is set properly')
      .isEqualTo(2)
    assertThat(CFG.'first1_level.second1_level.third3_value.40.3')
      .as('Check that new nested list map value is set properly')
      .isEqualTo(3)

    // Old values
    assertThat(CFG.'first1_level.second1_level.third3_value.1')
      .as('Check that modified nested list old values is still here')
      .isEqualTo(2)
    assertThat(CFG.'first1_level.second1_level.third3_value.34')
      .as('Check that modified nested list intermediate value is null')
      .isNull()
  }

  @Test
  void test_replace_list_map() throws Exception {
    CFG.'first1_level.second1_level.third3_value' = 'not-a-list'
    assertThat(CFG.'first1_level.second1_level.third3_value')
      .as('Check that replacing of list is working correctly')
      .isEqualTo('not-a-list')
    assertThat(CFG.'first1_level.second1_level.third3_value.1')
      .as('Check that replacing of list old values is not available')
      .isNull()

    CFG.'first1_level' = 432
    assertThat(CFG.'first1_level')
      .as('Check that replacing of map is working properly')
      .isEqualTo(432)
    assertThat(CFG.'first1_level.second1_level.third3_value')
      .as('Check that replacing of map old values is not available')
      .isNull()
  }
}
