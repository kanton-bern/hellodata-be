c.NotebookApp.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors localhost:8080",  # allow iframe set
        'Access-Control-Allow-Origin': 'localhost:8088',  # allow websockets from hello-data-jupyterhub-gateway
    }
}
