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
                    var_name='breed',
                    value_name="n_animals"
                )
    return df_data_new