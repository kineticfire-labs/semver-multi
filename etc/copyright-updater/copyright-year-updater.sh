#!/bin/env bash

# (c) Copyright 2024-2025 semver-multi Contributors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# KineticFire Labs: https://labs.kineticfire.com/
#	   Project site:  https://github.com/kineticfire-labs/semver-multi/


# KineticFire Labs: https://labs.kineticfire.com/
#	   project site: https://github.com/kineticfire-labs/clojure-collections/


# Usage:
#   - set 'year' to current year
#   - cd into the directory containing 'copyright-year-updater.sh'
#   - run './copyright-year-updater.sh'


term='semver-multi Contributors'
year='2025'



# searches for YYYY - YYYY
grep -Elsri "\(c\)[ ]+Copyright[ ]+[0-9]{4}[ ]*-[ ]*[0-9]{4}[ ]+${term}" ../../ \
   | xargs sed -Ei "s/\(c\)[ ]+Copyright[ ]+([0-9]{4})-[ ]*[0-9]{4}[ ]+${term}/(c) Copyright \1-${year} ${term}/" 2>/dev/null


# searches for YYYY
grep -Elsri "\(c\)[ ]+Copyright[ ]+[0-9]{4}[ ]+${term}" ../../ \
   | xargs sed -Ei "s/\(c\)[ ]+Copyright[ ]+([0-9]{4})[ ]+${term}/(c) Copyright \1-${year} ${term}/" 2>/dev/null
