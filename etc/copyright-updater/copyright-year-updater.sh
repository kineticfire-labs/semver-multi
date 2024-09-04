#!/bin/env bash

# (c) Copyright 2023-2024 KineticFire. All rights reserved.
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


# Usage:
#   - Set 'year' to current year
#   - cd into the directory containing 'copyright-year-updater.sh'
#   - Run './copyright-year-updater.sh'



year='2024'



# searches for YYYY - YYYY
grep -Elsri '\(c\)[ ]+Copyright[ ]+[0-9]{4}[ ]*-[ ]*[0-9]{4}[ ]+KineticFire' ../../ \
   | xargs sed -Ei "s/\(c\)[ ]+Copyright[ ]+([0-9]{4})-[ ]*[0-9]{4}[ ]+KineticFire/(c) Copyright \1-${year} KineticFire/"


# searches for YYYY
grep -Elsri '\(c\)[ ]+Copyright[ ]+[0-9]{4}[ ]+KineticFire' ../../ \
   | xargs sed -Ei "s/\(c\)[ ]+Copyright[ ]+([0-9]{4})[ ]+KineticFire/(c) Copyright \1-${year} KineticFire/"
