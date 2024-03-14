#!/usr/bin/python3
import getopt
import os
import shutil
import sys
import yaml
import zipfile


def unzip(zipfile_input, dashboard_export_path):
    # Create the destination directory if it doesn't exist
    if not os.path.exists(dashboard_export_path):
        os.makedirs(dashboard_export_path)

    # Extract the contents of the zip file to the destination path
    with zipfile.ZipFile(zipfile_input, 'r') as zip_ref:
        zip_ref.extractall(dashboard_export_path)


def delete_unzip_folder(dashboard_export_path):
    shutil.rmtree(dashboard_export_path, ignore_errors=False, onerror=None)


def get_yaml_attributes(filepath, extract_attribut):
    dict_attributes = {}
    with open(filepath, "r") as file:
        data = yaml.safe_load(file)

        for key in data.keys():
            if extract_attribut in key:
                dict_attributes[key] = data[key]
    return dict_attributes


def get_dict_from_yaml(dashboard_export_path):
    filepaths = [f.path for f in os.scandir(dashboard_export_path) if f.is_file()]
    dirpaths = [f.path for f in os.scandir(dashboard_export_path) if f.is_dir()]

    dict_files = {}
    for (dirpath, dirnames, filenames) in os.walk(dashboard_export_path):
        if not '.ipynb_checkpoints' in dirpath:

            list_files = []
            for filename in filenames:
                if 'metadata' in filename:
                    list_files.append({'filename': filename, 'attributes': get_yaml_attributes(os.path.join(dirpath, filename), 'type')})
                else:
                    list_files.append({'filename': filename, 'attributes': get_yaml_attributes(os.path.join(dirpath, filename), 'uuid')})
                dict_files[dirpath] = list_files
    return dict_files


def check_zip_export(export_path_dashboard, dict_files):
    list_error_output = []
    check_attribut = 'type'
    export_type = ''

    dickt_export_folders = {'Dashboard': ['dashboards', 'charts', 'databases', 'datasets'],
                            'Slice': ['charts', 'databases', 'datasets'],
                            'SqlaTable': ['databases', 'datasets'],
                            'Database': ['databases']}

    if not dict_files.get(export_path_dashboard):
        list_error_output.append(f'import file don`t match the check pattern')
    else:
        for file in dict_files.get(export_path_dashboard):
            filename = file.get('filename')
            if not check_attribut in file.get('attributes').keys():
                list_error_output.append(f'[ {check_attribut} ] is missing in {filename} : import file don`t match the check pattern')
            else:
                export_type = file.get('attributes').get('type')

    if not list_error_output:
        for check_folder in dickt_export_folders.get(export_type):
            if not list(filter(lambda x: check_folder in x, list(dict_files.keys()))):
                list_error_output.append(f'[ {check_folder} ] is missing in export zip file : import file don`t match the check pattern')

    return list_error_output


def check_if_not_attribut_exists(dict_files, check_folder, check_attribut):
    list_error_output = []
    for folder in dict_files:
        if check_folder in folder:
            for file in dict_files.get(folder):
                if not check_attribut in file.get('attributes').keys():
                    folder_name = folder.replace('export/', '')
                    list_error_output.append(f'[ {check_attribut} ] is missing in : {os.path.join(folder_name, file.get("filename"))}')

    return list_error_output


def main(argv):
    list_error_output = []

    zipfile_input = ''
    opts, args = getopt.getopt(argv, "hi:o:", ["ifile="])

    try:
        for opt, arg in opts:
            if opt == '-h':
                print('-i <inputfile>')
                sys.exit()
            elif opt in ("-i", "--ifile"):
                zipfile_input = arg

        print("zipfile_input: ", zipfile_input)
        export_path = '/tmp/unzip_export'
        print("export_path: ", export_path)
        export_path_dashboard = export_path + '/' + zipfile_input.replace("/", "_")
        print("export_path_dashboard: ", export_path_dashboard)
        unzip(zipfile_input, export_path_dashboard)

        folders_in_unzipped_dir = [name for name in os.listdir(export_path_dashboard) if os.path.isdir(os.path.join(export_path_dashboard, name))]
        if folders_in_unzipped_dir:
            print('folders', folders_in_unzipped_dir)
            export_path_dashboard = export_path_dashboard + '/' + folders_in_unzipped_dir[0]
            print('export path dashboard ' + export_path_dashboard)

        dict_files = get_dict_from_yaml(export_path_dashboard)

        list_error_output.extend(check_zip_export(export_path_dashboard, dict_files))
        list_error_output.extend(check_if_not_attribut_exists(dict_files, 'charts', 'dataset_uuid'))
        has_errors = False
        for error in list_error_output:
            print(error)
            has_errors = True

        delete_unzip_folder(export_path_dashboard)
        if len(has_errors) > 0:
            sys.exit(1)
    except FileNotFoundError as e:
        print("No such file or directory ### use -i [Path_to_File] ", str(e))
        sys.exit(1)
    except zipfile.BadZipFile:
        print("File is not a zip file")
    except Exception as e:
        print("Error occurred during validation", str(e))
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1:])
