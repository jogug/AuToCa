;;; annotation-osx-dcs.scm: OS X Dictionary Services functions for uim
;; comment

(define osx-dcs-ctx #f)

(define annotation-osx-dcs-init
  (lambda ()
    (if (require-dynlib "osx-dcs")
      (begin
        (set! osx-dcs-ctx #t)
        #t)
      #f)))

(define annotation-osx-dcs-get-text
  (lambda (text enc)
    (or (and osx-dcs-ctx
             (osx-dcs-search-text text enc))
        "")))

(define annotation-osx-dcs-release
  (lambda ()
    (if osx-dcs-ctx
      (begin
        (set! osx-dcs-ctx #f)
        (dynlib-unload "osx-dcs"))
      #f)))
