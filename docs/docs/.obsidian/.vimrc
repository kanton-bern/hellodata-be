

" remap esc key
imap kj <Esc>
imap jk <Esc>
imap jj <Esc>

" map to visual line instead of full line as in obsidian line are always
" wraped
noremap <C-j> j
noremap <C-k> k
nmap j gj
nmap k gk
" nmap $ g$
" nmap ^ g^
" nmap 0 g^

" better scrolling and searching with centered always
noremap <c-d> <C-d>zz
noremap <c-u> <C-u>zz
noremap <n> nzzzv
noremap <N> Nzzzv

" set scrolloff=8

"yank to clipboard
set clipboard=unnamed
