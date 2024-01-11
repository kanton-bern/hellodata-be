#
# Copyright © 2024, Kanton Bern
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

import re

def snake_case(s: str):
    """
    Converts a string to snake_case

    :param str s: The string to convert to snake case
    :return: Input string converted to snake case
    :rtype: str

    Example input:  "get2HTTPResponse123Code_Style _  Manace- LIKE"
    Example output: "get2_http_response123_code_style_manace_like"
    """
    pattern = re.compile('((?<=[a-z0-9])[A-Z]|(?!^)(?<!_)[A-Z](?=[a-z]))')
    #a = "_".join( s.replace('-', ' ').replace('_', ' ').replace("'", " ").replace(',', ' ').replace('(', ' ').replace(')', ' ').split() )
    a=re.sub("[^A-Za-z0-9_]","",s)
    return pattern.sub(r'_\1', a).lower()

def df_breed_melt(df_data):
    df_data_new = df_data.melt(
                    id_vars=["Year","Month"],
                    var_name=["breed"],
                    value_name="n_animals"
                )
    return df_data_new
